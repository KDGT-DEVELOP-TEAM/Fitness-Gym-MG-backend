package com.example.fitnessgym_mg.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.fitnessgym_mg.dto.request.LoginRequest;
import com.example.fitnessgym_mg.exception.ErrorResponse;

import jakarta.annotation.PostConstruct;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ログイン試行回数制限フィルター
 * 
 * <p>責務:</p>
 * <ul>
 *   <li>IPアドレスとメールアドレスの組み合わせでログイン試行回数を制限</li>
 *   <li>メールアドレスベースのレート制限を提供（LoginRateLimitFilterはIPアドレスのみ）</li>
 * </ul>
 * 
 * <p>LoginRateLimitFilterとの違い:</p>
 * <ul>
 *   <li>LoginRateLimitFilter: IPアドレスのみでレート制限（より広範囲な制限）</li>
 *   <li>LoginAttemptFilter: IPアドレス + メールアドレスの組み合わせでレート制限（より細かい制限）</li>
 * </ul>
 * 
 * <p>セキュリティ注意事項:</p>
 * <ul>
 *   <li>メールアドレスはハッシュ化してからキャッシュキーに使用（PII保護）</li>
 *   <li>ForwardedHeaderFilterが適切に設定されていることを前提とする</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptFilter extends OncePerRequestFilter {
    
    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    
    @Value("${login.rate-limit.max-attempts:5}")
    private int maxAttempts;
    
    @Value("${login.rate-limit.time-window-minutes:5}")
    private int timeWindowMinutes;
    
    private LoadingCache<String, AtomicInteger> attemptsCache;
    
    private final ObjectMapper objectMapper;
    
    @PostConstruct
    public void init() {
        // @Valueで注入された値を使用してキャッシュを初期化
        // AtomicIntegerを使用して原子操作による並行性を保証
        attemptsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(timeWindowMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<String, AtomicInteger>() {
                    @Override
                    public AtomicInteger load(String key) {
                        return new AtomicInteger(0);
                    }
                });
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                     HttpServletResponse response, 
                                     FilterChain filterChain) throws ServletException, IOException {
        
        // フィルターの最初で全リクエストをラップ（後続フィルターでもリクエストボディを読み取れるようにする）
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        
        if (wrappedRequest.getRequestURI().equals(LOGIN_ENDPOINT) 
                && wrappedRequest.getMethod().equals("POST")) {
            
            String clientIP = getClientIP(wrappedRequest);
            String email = extractEmailFromRequest(wrappedRequest);
            String cacheKey = generateCacheKey(clientIP, email, wrappedRequest);
            
            // レート制限チェック
            int attempts = 0;
            try {
                AtomicInteger attemptsCounter = attemptsCache.get(cacheKey);
                attempts = attemptsCounter.get();
            } catch (com.google.common.cache.CacheException e) {
                // キャッシュの永続的なエラーの場合のみブロック
                log.error("Rate limit check failed for cacheKey: {}", maskCacheKey(cacheKey), e);
                sendRateLimitExceeded(response, timeWindowMinutes);
                return;
            } catch (Exception e) {
                // 一時的なエラーの場合は警告ログを出力し、レート制限をスキップ
                log.warn("Temporary error in rate limit check for cacheKey: {}, allowing request", 
                    maskCacheKey(cacheKey), e);
                // レート制限をスキップして続行
            }
            
            // レート制限チェック（例外が発生しなかった場合のみ）
            if (attempts >= maxAttempts) {
                sendRateLimitExceeded(response, timeWindowMinutes);
                return;
            }
            
            // 試行回数をインクリメント（原子操作）
            try {
                attemptsCache.get(cacheKey).incrementAndGet();
            } catch (com.google.common.cache.CacheException e) {
                // キャッシュの永続的なエラーの場合のみブロック
                log.error("Failed to increment attempts cache for cacheKey: {}", 
                    maskCacheKey(cacheKey), e);
                sendRateLimitExceeded(response, timeWindowMinutes);
                return;
            } catch (Exception e) {
                // 一時的なエラーの場合は警告ログを出力し、レート制限をスキップ
                log.warn("Temporary error incrementing attempts cache for cacheKey: {}, allowing request", 
                    maskCacheKey(cacheKey), e);
                // レート制限をスキップして続行
            }
        }
        
        // 全リクエストに対してラップしたリクエストを次のフィルターに渡す
        filterChain.doFilter(wrappedRequest, response);
    }
    
    /**
     * クライアントIPアドレスを取得
     *
     * <p>セキュリティ注意事項:</p>
     * <ul>
     *   <li>ForwardedHeaderFilterが適切に設定されていることを前提とする</li>
     *   <li>信頼できないプロキシからのX-Forwarded-Forヘッダーは使用しない</li>
     *   <li>本番環境では、信頼できるプロキシのIPアドレスを明示的に設定すること</li>
     * </ul>
     *
     * <p>注意: この実装はForwardedHeaderFilterがX-Forwarded-Forヘッダーを
     * 適切に検証していることを前提としています。信頼できないプロキシからの
     * リクエストでは、X-Forwarded-Forヘッダーが無視される設定が必要です。</p>
     * 
     * @param request HTTPリクエスト
     * @return クライアントIPアドレス
     */
    private String getClientIP(HttpServletRequest request) {
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

        // フォールバック: 元のIPアドレスを返す（X-Forwarded-Forは使用しない）
        return request.getRemoteAddr() != null && !request.getRemoteAddr().isEmpty() 
            ? request.getRemoteAddr() 
            : "unknown";
    }
    
    /**
     * リクエストボディからemailを取得
     * 
     * <p>パフォーマンス注意事項:</p>
     * <ul>
     *   <li>リクエストボディのパースは最小限に抑える</li>
     *   <li>メールアドレスの取得に失敗した場合は、早期リターンしてIPアドレスのみを使用</li>
     * </ul>
     * 
     * @param request ContentCachingRequestWrapperでラップされたリクエスト
     * @return email（取得できない場合はnull）
     */
    private String extractEmailFromRequest(ContentCachingRequestWrapper request) {
        try {
            byte[] content = request.getContentAsByteArray();
            if (content.length == 0) {
                return null;
            }
            
            // リクエストボディのパース（最小限）
            String body = new String(content, StandardCharsets.UTF_8);
            LoginRequest loginRequest = objectMapper.readValue(body, LoginRequest.class);
            String email = loginRequest.getEmail();
            
            // 空文字列もnullとして扱う
            return (email != null && !email.isEmpty()) ? email : null;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // JSONパースエラーの場合は警告のみ（IPアドレスのみでレート制限）
            log.debug("Failed to parse login request body, using IP-only rate limiting", e);
            return null;
        } catch (Exception e) {
            // その他のエラーの場合も警告のみ
            log.warn("Failed to extract email from request body, using IP-only rate limiting", e);
            return null;
        }
    }
    
    /**
     * リクエストボディのハッシュを計算
     * 
     * @param content リクエストボディのバイト配列
     * @return SHA-256ハッシュの16進数文字列
     */
    private String calculateHash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to calculate hash", e);
            // フォールバック: コンテンツの長さと最初の数バイトを使用
            if (content.length > 0) {
                return Integer.toHexString(content.length) + Integer.toHexString(content[0] & 0xFF);
            }
            return "0";
        }
    }
    
    /**
     * キャッシュキーを生成
     * 
     * <p>セキュリティ注意事項:</p>
     * <ul>
     *   <li>メールアドレスはハッシュ化してからキャッシュキーに使用（PII保護）</li>
     *   <li>メールアドレスが取得できない場合は、リクエストボディのハッシュまたはIPアドレスのみを使用</li>
     * </ul>
     * 
     * @param clientIP クライアントIPアドレス
     * @param email メールアドレス（取得できない場合はnull）
     * @param request ContentCachingRequestWrapperでラップされたリクエスト
     * @return キャッシュキー
     */
    private String generateCacheKey(String clientIP, String email, ContentCachingRequestWrapper request) {
        if (email != null && !email.isEmpty()) {
            // emailが取得できる場合: ハッシュ化してから使用（PII保護）
            String emailHash = hashEmail(email.toLowerCase());
            return clientIP + ":" + emailHash;
        }
        
        // emailが取得できない場合: リクエストボディのハッシュを使用（パフォーマンス最適化）
        try {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                String hash = calculateHash(content);
                return clientIP + ":" + hash.substring(0, Math.min(16, hash.length()));
            }
        } catch (Exception e) {
            log.debug("Failed to calculate hash for cache key, using IP-only", e);
        }
        
        // リクエストボディが空の場合: IPのみを使用
        return clientIP;
    }
    
    /**
     * メールアドレスをSHA-256ハッシュ化（キャッシュキー用）
     * 
     * <p>個人情報保護のため、キャッシュキーにはメールアドレスを直接使用せず、ハッシュ値を使用する。</p>
     * 
     * @param email メールアドレス
     * @return SHA-256ハッシュ値（16進数文字列）
     */
    private String hashEmail(String email) {
        if (email == null) {
            return "null";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(email.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256は標準アルゴリズムなので、この例外は発生しないはず
            log.error("SHA-256 algorithm not found", e);
            return "hash_error";
        }
    }
    
    /**
     * レートリミット超過エラーレスポンスを返す
     * 
     * <p>ErrorResponseクラスを使用してエラーレスポンス形式を統一する。</p>
     */
    private void sendRateLimitExceeded(HttpServletResponse response, int timeWindowMinutes) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse errorResponse = new ErrorResponse(
            "RATE_LIMIT_EXCEEDED",
            "ログイン試行回数が上限に達しました。" + timeWindowMinutes + "分後に再試行してください"
        );
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }
    
    /**
     * キャッシュキーをマスク（ログ出力用）
     * 
     * <p>セキュリティ上の観点から、ログには個人情報を含めないようにする。</p>
     * メールアドレスのハッシュ部分をマスクする。
     * 
     * @param cacheKey キャッシュキー
     * @return マスクされたキャッシュキー
     */
    private String maskCacheKey(String cacheKey) {
        if (cacheKey == null || !cacheKey.contains(":")) {
            return cacheKey;
        }
        String[] parts = cacheKey.split(":", 2);
        if (parts.length == 2) {
            // IPアドレス部分はそのまま、ハッシュ部分は最初の8文字のみ表示
            String hashPart = parts[1];
            if (hashPart.length() > 8) {
                return parts[0] + ":" + hashPart.substring(0, 8) + "***";
            }
        }
        return cacheKey;
    }
}

