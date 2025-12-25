package com.example.fitnessgym_mg.config.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ログインエンドポイント用レートリミットフィルター
 * ブルートフォース攻撃を防ぐため、5分間に5回までのログイン試行を許可
 */
@Slf4j
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration TIME_WINDOW = Duration.ofMinutes(5);

    // IPアドレスごとのバケットを保持
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // ログインエンドポイントのPOSTリクエストのみに適用
        if (LOGIN_ENDPOINT.equals(request.getRequestURI()) 
                && "POST".equals(request.getMethod())) {

            String clientIp = getClientIp(request);
            Bucket bucket = cache.computeIfAbsent(clientIp, k -> createBucket());

            // レートリミットチェック
            if (!bucket.tryConsume(1)) {
                log.warn("レートリミット超過: IP={}, エンドポイント={}", clientIp, LOGIN_ENDPOINT);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(
                        "{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"リクエストが多すぎます。しばらく時間をおいてから再度お試しください。\"}"
                );
                response.getWriter().flush();
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * クライアントIPアドレスを取得
     * X-Forwarded-Forヘッダーを考慮（リバースプロキシ経由の場合）
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // カンマ区切りの場合、最初のIPを取得
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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

