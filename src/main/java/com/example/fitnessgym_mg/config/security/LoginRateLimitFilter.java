package com.example.fitnessgym_mg.config.security;

import com.example.fitnessgym_mg.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ログインエンドポイントとパスワードリセットリクエストエンドポイント用レートリミットフィルター
 * ブルートフォース攻撃やDoS攻撃を防ぐため、5分間に5回までのリクエストを許可
 * 
 * <p>パフォーマンス注意事項:</p>
 * <ul>
 *   <li>バケットは定期的にクリーンアップされる（10分ごと）</li>
 *   <li>長時間使用されていないIPアドレスのバケットは自動的に削除される</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final String PASSWORD_RESET_REQUEST_ENDPOINT = "/api/password-reset/request";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration TIME_WINDOW = Duration.ofMinutes(5);
    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(10);

    private final ObjectMapper objectMapper;

    // IPアドレスごとのバケットを保持
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    
    // バケットの最終アクセス時刻を記録
    private final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService cleanupScheduler = 
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "login-rate-limit-cleanup");
            t.setDaemon(true);
            return t;
        });

    @PostConstruct
    public void init() {
        // 定期的にクリーンアップを実行
        cleanupScheduler.scheduleAtFixedRate(
            this::cleanupExpiredBuckets,
            CLEANUP_INTERVAL.toMinutes(),
            CLEANUP_INTERVAL.toMinutes(),
            TimeUnit.MINUTES
        );
    }
    
    @PreDestroy
    public void destroy() {
        log.info("LoginRateLimitFilterのクリーンアップスケジューラーをシャットダウン中...");
        cleanupScheduler.shutdown();
        try {
            // 最大30秒待機してシャットダウンを完了させる
            if (!cleanupScheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("クリーンアップスケジューラーのシャットダウンが30秒以内に完了しませんでした。強制終了します。");
                cleanupScheduler.shutdownNow();
                // さらに10秒待機
                if (!cleanupScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("クリーンアップスケジューラーの強制終了に失敗しました。");
                }
            }
        } catch (InterruptedException e) {
            log.warn("クリーンアップスケジューラーのシャットダウン待機中に割り込みが発生しました。強制終了します。", e);
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("LoginRateLimitFilterのクリーンアップスケジューラーをシャットダウンしました。");
    }
    
    /**
     * 期限切れのバケットをクリーンアップ
     * 最後のアクセスから10分以上経過したバケットを削除
     * 
     * <p>例外が発生してもスケジューラーを継続させるため、すべての例外をキャッチしてログに記録します。</p>
     */
    private void cleanupExpiredBuckets() {
        try {
            long now = System.currentTimeMillis();
            long expireTime = CLEANUP_INTERVAL.toMillis();
            
            // 削除対象のIPアドレスを事前に収集
            Set<String> expiredIps = new HashSet<>();
            for (Map.Entry<String, Long> entry : lastAccessTime.entrySet()) {
                String ip = entry.getKey();
                Long lastAccess = entry.getValue();
                if (lastAccess == null || (now - lastAccess) > expireTime) {
                    expiredIps.add(ip);
                }
            }
            
            // 削除対象のバケットを削除
            for (String ip : expiredIps) {
                cache.remove(ip);
                lastAccessTime.remove(ip);
                log.debug("レートリミットバケットをクリーンアップ: IP={}", ip);
            }
            
            if (!expiredIps.isEmpty()) {
                log.debug("レートリミットバケットのクリーンアップ完了: {}件削除", expiredIps.size());
            }
        } catch (Exception e) {
            // 例外が発生してもスケジューラーを継続させる
            log.error("レートリミットバケットのクリーンアップ中にエラーが発生しました", e);
        }
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // ログインエンドポイントまたはパスワードリセットリクエストエンドポイントのPOSTリクエストに適用
        if ("POST".equals(method) && 
            (LOGIN_ENDPOINT.equals(requestUri) || PASSWORD_RESET_REQUEST_ENDPOINT.equals(requestUri))) {

            String clientIp = getClientIp(request);
            Bucket bucket = cache.computeIfAbsent(clientIp, k -> createBucket());
            
            // アクセス時刻を更新
            lastAccessTime.put(clientIp, System.currentTimeMillis());

            // レートリミットチェック
            if (!bucket.tryConsume(1)) {
                log.warn("レートリミット超過: IP={}, エンドポイント={}", clientIp, requestUri);
                sendRateLimitExceeded(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * クライアントIPアドレスを取得
     * 
     * <p>セキュリティ注意事項:</p>
     * <ul>
     *   <li>X-Forwarded-Forヘッダーは信頼できるプロキシからのみ使用すべき</li>
     *   <li>ForwardedHeaderFilterが適切に設定されていることを前提とする</li>
     *   <li>本番環境では、信頼できるプロキシのIPアドレスを明示的に設定すること</li>
     * </ul>
     * 
     * <p>注意: この実装はForwardedHeaderFilterがX-Forwarded-Forヘッダーを
     * 適切に検証していることを前提としています。信頼できないプロキシからの
     * リクエストでは、X-Forwarded-Forヘッダーが無視される設定が必要です。</p>
     */
    private String getClientIp(HttpServletRequest request) {
        // ForwardedHeaderFilterが設定されている場合、request.getRemoteAddr()は
        // 信頼できるプロキシからのX-Forwarded-Forヘッダーの値を返す
        // 信頼できないプロキシからのリクエストでは、元のIPアドレスが返される
        String remoteAddr = request.getRemoteAddr();
        
        // 追加の検証: IPアドレスの形式チェック（簡易版）
        if (remoteAddr != null && !remoteAddr.isEmpty()) {
            // IPv4またはIPv6の形式チェック（簡易版）
            if (remoteAddr.matches("^([0-9]{1,3}\\.){3}[0-9]{1,3}$") || 
                remoteAddr.matches("^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$")) {
                return remoteAddr;
            }
        }
        
        // フォールバック: 元のIPアドレスを返す
        return request.getRemoteAddr();
    }
    
    /**
     * レートリミット超過エラーレスポンスを返す
     */
    private void sendRateLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "RATE_LIMIT_EXCEEDED",
            "リクエストが多すぎます。しばらく時間をおいてから再度お試しください。"
        );
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }

    /**
     * レートリミット用バケットを作成
     * 5分間に5回までのリクエストを許可
     */
    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(MAX_ATTEMPTS)
                        .refillIntervally(MAX_ATTEMPTS, TIME_WINDOW))
                .build();
    }
}

