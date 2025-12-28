package com.example.fitnessgym_mg.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.fitnessgym_mg.dto.request.LoginRequest;

import jakarta.annotation.PostConstruct;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoginAttemptFilter extends OncePerRequestFilter {
    
    @Value("${login.rate-limit.max-attempts:5}")
    private int maxAttempts;
    
    @Value("${login.rate-limit.time-window-minutes:5}")
    private int timeWindowMinutes;
    
    private LoadingCache<String, AtomicInteger> attemptsCache;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
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
        
        if (wrappedRequest.getRequestURI().equals("/api/auth/login") 
                && wrappedRequest.getMethod().equals("POST")) {
            
            String clientIP = getClientIP(wrappedRequest);
            String email = extractEmailFromRequest(wrappedRequest);
            String cacheKey = generateCacheKey(clientIP, email, wrappedRequest);
            
            // 失敗安全: 例外時は制限として扱う（ブロック）
            int attempts = maxAttempts;
            try {
                AtomicInteger attemptsCounter = attemptsCache.get(cacheKey);
                attempts = attemptsCounter.get();
            } catch (Exception e) {
                log.error("Rate limit check failed for cacheKey: {}", cacheKey, e);
                // attemptsはmaxAttemptsのまま（ブロック）
            }
            
            // レート制限チェック（例外発生時も実行）
            if (attempts >= maxAttempts) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                    "{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"ログイン試行回数が上限に達しました。" + timeWindowMinutes + "分後に再試行してください\"}"
                );
                return;
            }
            
            // 試行回数をインクリメント（原子操作）
            try {
                attemptsCache.get(cacheKey).incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to increment attempts cache for cacheKey: {}", cacheKey, e);
                // インクリメント失敗時もブロック（安全側）
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                    "{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"ログイン試行回数が上限に達しました。" + timeWindowMinutes + "分後に再試行してください\"}"
                );
                return;
            }
        }
        
        // 全リクエストに対してラップしたリクエストを次のフィルターに渡す
        filterChain.doFilter(wrappedRequest, response);
    }
    
    /**
     * クライアントIPアドレスを取得
     * ForwardedHeaderFilterが処理済みのIPを優先的に使用
     * 
     * @param request HTTPリクエスト
     * @return クライアントIPアドレス
     */
    private String getClientIP(HttpServletRequest request) {
        // ForwardedHeaderFilterが処理済みのIPを取得
        // X-Forwarded-Forヘッダーは既に処理されているため、直接RemoteAddrを使用
        String clientIP = request.getRemoteAddr();
        
        // フォールバック: X-Forwarded-Forヘッダーが直接設定されている場合（信頼できない環境）
        // ただし、ForwardedHeaderFilterが有効な場合は通常は不要
        if (clientIP == null || clientIP.isEmpty() || "127.0.0.1".equals(clientIP) || "0:0:0:0:0:0:0:1".equals(clientIP)) {
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.isEmpty()) {
                clientIP = xfHeader.split(",")[0].trim();
            }
        }
        
        return clientIP != null && !clientIP.isEmpty() ? clientIP : "unknown";
    }
    
    /**
     * リクエストボディからemailを取得
     * 
     * @param request ContentCachingRequestWrapperでラップされたリクエスト
     * @return email（取得できない場合はnull）
     */
    private String extractEmailFromRequest(ContentCachingRequestWrapper request) {
        try {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, StandardCharsets.UTF_8);
                LoginRequest loginRequest = objectMapper.readValue(body, LoginRequest.class);
                String email = loginRequest.getEmail();
                // 空文字列もnullとして扱う
                return (email != null && !email.isEmpty()) ? email : null;
            }
        } catch (Exception e) {
            log.warn("Failed to extract email from request body", e);
        }
        return null;
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
     * @param clientIP クライアントIPアドレス
     * @param email メールアドレス（取得できない場合はnull）
     * @param request ContentCachingRequestWrapperでラップされたリクエスト
     * @return キャッシュキー
     */
    private String generateCacheKey(String clientIP, String email, ContentCachingRequestWrapper request) {
        if (email != null && !email.isEmpty()) {
            // emailが取得できる場合: 小文字化して使用
            return clientIP + ":" + email.toLowerCase();
        }
        
        // emailが取得できない場合: リクエストボディのハッシュを使用
        try {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                String hash = calculateHash(content);
                return clientIP + ":" + hash.substring(0, Math.min(16, hash.length()));
            }
        } catch (Exception e) {
            log.warn("Failed to calculate hash for cache key", e);
        }
        
        // リクエストボディが空の場合: IPのみを使用
        return clientIP;
    }
}

