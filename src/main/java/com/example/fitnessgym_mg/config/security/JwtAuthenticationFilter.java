package com.example.fitnessgym_mg.config.security;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.UserRepository;
import com.example.fitnessgym_mg.util.JwtTokenUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import jakarta.annotation.PostConstruct;
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
 * - ユーザー情報のキャッシュ（パフォーマンス向上）
 *
 * <p>エラーレスポンス生成はJwtAuthenticationEntryPointに分離されています。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @PostConstruct
    public void init() {
        // ユーザー情報キャッシュを初期化
        // デフォルト5分間キャッシュを保持（環境変数で変更可能）
        userCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheExpirationSeconds, TimeUnit.SECONDS)
                .maximumSize(10000) // 最大10,000ユーザーをキャッシュ
                .recordStats() // 統計情報を記録（デバッグ用）
                .build();
        
        log.info("JWT認証フィルターのユーザーキャッシュを初期化: 有効期限={}秒", cacheExpirationSeconds);
    }

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    
    @Value("${jwt.user-cache.expiration-seconds:300}")
    private int cacheExpirationSeconds;
    
    // ユーザー情報キャッシュ（パフォーマンス向上のため）
    // キー: userId (UUID), 値: Userエンティティ
    private Cache<UUID, User> userCache;

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

            // subject（userId）からユーザーIDを取得
            String subject = claims.getSubject();
            if (!StringUtils.hasText(subject)) {
                log.warn("JWTトークンにsubjectが含まれていません");
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.sendUnauthorized(response, "JWT_INVALID", "JWTトークンが無効です");
                return;
            }

            UUID userId;
            try {
                userId = UUID.fromString(subject);
            } catch (IllegalArgumentException e) {
                log.warn("JWTトークンのsubjectが無効なUUID形式です: {}", subject);
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.sendUnauthorized(response, "JWT_INVALID", "JWTトークンが無効です");
                return;
            }

            // ユーザーIDからUserエンティティを取得
            // セキュリティ改善: emailをJWTから削除し、DBから取得することで個人情報漏洩リスクを低減
            // パフォーマンス改善: キャッシュから取得を試み、見つからない場合のみDBアクセス
            User user = userCache.get(userId, key -> {
                log.debug("ユーザーキャッシュミス: userId={}, DBから取得", key);
                return userRepository.findById(key).orElse(null);
            });
            
            // キャッシュにnullが保存される可能性があるため、明示的にチェック
            if (user == null) {
                // キャッシュにnullが保存されている可能性があるため、キャッシュを無効化して再取得
                userCache.invalidate(userId);
                user = userRepository.findById(userId).orElse(null);
            }

            // ユーザーが存在しない、または無効化されている場合はエラー
            if (user == null || !user.isActive()) {
                log.warn("JWT認証失敗: ユーザーが無効化されているか存在しません. userId={}", userId);
                // キャッシュを無効化（無効化されたユーザー情報がキャッシュに残らないようにする）
                if (user != null) {
                    userCache.invalidate(userId);
                }
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.sendUnauthorized(response, "JWT_USER_INACTIVE", "ユーザーアカウントが無効化されています");
                return;
            }

            // Claimsからロールを取得（DBの値と整合性チェックは行わない）
            String role = claims.get("role", String.class);

            if (!StringUtils.hasText(role)) {
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.sendUnauthorized(response, "JWT_MISSING_ROLE", "JWTトークンにroleが含まれていません");
                return;
            }

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority("ROLE_" + role);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user.getEmail(),
                                null,
                                Collections.singletonList(authority)
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT認証成功: email={}, role={}", user.getEmail(), role);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.warn("JWT期限切れ: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.sendUnauthorized(response, "JWT_EXPIRED", "JWTトークンの有効期限が切れています");
        } catch (JwtException | IllegalArgumentException e) {
            // 署名不正・形式不正・改ざん・サポート外など
            // 注意: 高頻度で発生する可能性があるため、スタックトレースは出力しない
            log.warn("JWT無効: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.sendUnauthorized(response, "JWT_INVALID", "JWTトークンが無効です");
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
     * 
     * POST /api/auth/login のみJWTフィルターをスキップ
     * GET /api/auth/me と POST /api/auth/logout は認証が必要なため、JWTフィルターを通過させる
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        // POST /api/auth/login のみJWTフィルターをスキップ
        return uri.equals("/api/auth/login") && "POST".equals(method);
    }

}
