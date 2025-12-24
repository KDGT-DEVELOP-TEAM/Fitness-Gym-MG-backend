package com.example.fitnessgym_mg.config.security;

import java.io.IOException;
import java.util.Collections;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.fitnessgym_mg.util.JwtTokenUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT認証フィルター
 * 
 * リクエストヘッダーからJWTトークンを抽出し、検証して認証情報を設定します。
 * Authorization: Bearer <token> 形式のヘッダーを期待します。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Authorizationヘッダーからトークンを抽出
            String token = extractToken(request);

            // トークンが存在し、有効な場合
            if (StringUtils.hasText(token) && jwtTokenUtil.validateToken(token)) {
                // トークンから情報を取得
                String email = jwtTokenUtil.getEmailFromToken(token);
                String role = jwtTokenUtil.getRoleFromToken(token);

                // Spring Security用の権限を作成（ROLE_プレフィックスを付与）
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                // 認証トークンを作成
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                Collections.singletonList(authority)
                        );

                // SecurityContextに認証情報を設定
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT認証成功: email={}, role={}", email, role);
            }
        } catch (Exception e) {
            log.error("JWT認証処理でエラーが発生しました: {}", e.getMessage());
            // エラーが発生しても、次のフィルターに進む（認証されていない状態として処理される）
        }

        // 次のフィルターに進む
        filterChain.doFilter(request, response);
    }

    /**
     * リクエストヘッダーからJWTトークンを抽出
     * 
     * @param request HTTPリクエスト
     * @return JWTトークン（Bearerプレフィックスなし）、存在しない場合はnull
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * 認証が不要なパスかどうかを判定
     * /api/auth/** は認証不要
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // ログインエンドポイントはフィルタリングしない（認証不要）
        return path.startsWith("/api/auth/login");
    }
}
