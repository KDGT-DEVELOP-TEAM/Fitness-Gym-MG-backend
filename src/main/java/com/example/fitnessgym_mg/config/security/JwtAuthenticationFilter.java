package com.example.fitnessgym_mg.config.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.fitnessgym_mg.exception.ErrorResponse;
import com.example.fitnessgym_mg.util.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT認証フィルター
 * 
 * - JWTの検証
 * - 認証情報の SecurityContext への設定
 *
 * ※ レスポンス生成は将来的に AuthenticationEntryPoint へ分離可能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenUtil jwtTokenUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

            String token = extractToken(request);

        // JWT未送信の場合は何もしない（未認証アクセスとして扱う）
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // トークンを1回だけパースして検証し、Claimsを取得
            Claims claims = jwtTokenUtil.parseAndValidate(token);

            // 取得したClaimsから情報を取得
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);

            if (!StringUtils.hasText(role)) {
                sendUnauthorized(response, "JWT_MISSING_ROLE", "JWTトークンにroleが含まれていません");
                return;
            }

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority("ROLE_" + role);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                Collections.singletonList(authority)
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT認証成功: email={}, role={}", email, role);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.warn("JWT期限切れ: {}", e.getMessage());
            sendUnauthorized(response, "JWT_EXPIRED", "JWTトークンの有効期限が切れています");
        } catch (JwtException | IllegalArgumentException e) {
            // 署名不正・形式不正・改ざん・サポート外など
            log.error("JWT無効: {}", e.getMessage(), e);
            sendUnauthorized(response, "JWT_INVALID", "JWTトークンが無効です");
        }
    }

    /**
     * AuthorizationヘッダーからJWTを取得
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 認証不要パス判定
     *
     * ※ permitAll() 設定と必ず同期させること
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/auth/");
    }

    /**
     * 401 エラーレスポンスを返す
     *
     * ※ 将来的に AuthenticationEntryPoint へ移行可能
     */
    private void sendUnauthorized(
            HttpServletResponse response,
            String errorCode,
            String message
    ) throws IOException {

        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse errorResponse = new ErrorResponse(errorCode, message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
    }
}
