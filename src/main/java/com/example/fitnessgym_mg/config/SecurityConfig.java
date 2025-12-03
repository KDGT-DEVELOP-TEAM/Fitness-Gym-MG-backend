package com.example.fitnessgym_mg.config;

import java.io.IOException;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Security設定クラス
 * 
 * 認証・認可の設定を担当します。
 * ログイン処理の流れ：
 * 1. ユーザーがログインフォームにメールアドレスとパスワードを入力
 * 2. CustomUserDetailsServiceでユーザー情報を取得
 * 3. パスワードをBCryptで比較して認証
 * 4. 認証成功後、権限（ROLE）に応じて適切な画面にリダイレクト
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final UserRepository userRepository;

    /**
     * パスワードエンコーダーのBean定義
     * パスワードをBCryptでハッシュ化するためのエンコーダーを返します
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 認証マネージャーのBean定義
     * ログイン処理を実行するためのマネージャーを返します
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * セキュリティフィルターチェーンの設定
     * 
     * 処理内容：
     * 1. 認可設定：静的リソースとログイン画面は認証不要、その他は認証必須
     * 2. フォームログイン設定：ログインページ、処理URL、成功時のリダイレクト先
     * 3. ログアウト設定：ログアウトURL、成功時のリダイレクト先
     * 4. CSRF対策：APIエンドポイントはCSRFチェックを無視
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 認可設定：静的リソースとログイン画面は認証不要、その他は認証必須
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
            )
            // フォームログイン設定
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(customAuthenticationSuccessHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )
            // ログアウト設定
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // CSRF対策：APIエンドポイントと有効/無効エンドポイントはCSRFチェックを無視
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                "/api/**",
                // 顧客の有効/無効エンドポイント
                "/admin/customers/*/enable",
                "/admin/customers/*/disable",
                "/manager/*/customers/*/enable",
                "/manager/*/customers/*/disable",
                // ユーザーの有効/無効エンドポイント
                "/admin/users/*/enable",
                "/admin/users/*/disable",
                "/manager/*/users/*/enable",
                "/manager/*/users/*/disable"
            ));

        return http.build();
    }

    /**
     * ログイン成功後の権限ベースリダイレクトハンドラー
     * 
     * 処理の流れ：
     * 1. ユーザーの権限を取得
     * 2. 権限に応じてリダイレクト先を決定
     *    - ROLE_ADMIN → /admin/dashboard
     *    - ROLE_MANAGER → /manager/{storeId}/lessons（店舗統計画面）
     *    - ROLE_TRAINER → /trainer/customers
     * 3. 決定したURLにリダイレクト
     */
    @Bean
    AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                    Authentication authentication) throws IOException, ServletException {
                
                String email = authentication.getName();
                String redirectUrl = "/";
                
                // デバッグログ: 認証されたユーザーの権限を出力
                log.info("認証成功: ユーザー={}, 権限={}", email, authentication.getAuthorities());
                
                // 権限に応じてリダイレクト先を決定
                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
                boolean isManager = authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_MANAGER"));
                boolean isTrainer = authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_TRAINER"));
                
                log.info("権限判定結果: ADMIN={}, MANAGER={}, TRAINER={}", isAdmin, isManager, isTrainer);
                
                if (isAdmin) {
                    redirectUrl = "/admin/dashboard";
                    log.info("ADMINユーザーとしてリダイレクト: {}", redirectUrl);
                } else if (isManager) {
                    // 店長の場合は所属店舗IDを取得して統計画面にリダイレクト
                    try {
                        redirectUrl = getManagerRedirectUrl(email);
                        log.info("MANAGERユーザーとしてリダイレクト: {}", redirectUrl);
                    } catch (Exception e) {
                        log.error("店長のリダイレクトURL取得に失敗: ユーザー={}, エラー={}", email, e.getMessage(), e);
                        // 例外が発生した場合はログイン画面にリダイレクト（エラーメッセージ付き）
                        response.sendRedirect("/login?error=manager_redirect_failed");
                        return;
                    }
                } else if (isTrainer) {
                    redirectUrl = "/trainer/customers";
                    log.info("TRAINERユーザーとしてリダイレクト: {}", redirectUrl);
                } else {
                    log.warn("未知の権限: ユーザー={}, 権限={}", email, authentication.getAuthorities());
                }
                
                log.info("最終リダイレクト先: {}", redirectUrl);
                response.sendRedirect(redirectUrl);
            }
        };
    }

    /**
     * 店長のリダイレクトURLを取得
     * 所属店舗IDを取得して統計画面（/manager/{storeId}/lessons）にリダイレクト
     * 
     * @param email ログインユーザーのメールアドレス
     * @return リダイレクトURL
     */
    private String getManagerRedirectUrl(String email) {
        log.info("店長のリダイレクトURL取得開始: ユーザー={}", email);
        
        // ユーザー情報を取得（storesをJOIN FETCH）
        User user = userRepository.findByEmailWithStores(email)
                .orElseThrow(() -> {
                    log.error("ユーザーが見つかりません: {}", email);
                    return new RuntimeException("ユーザーが見つかりません: " + email);
                });

        log.info("ユーザー情報取得成功: ユーザー={}, 権限={}, 店舗数={}", 
                email, user.getRole(), 
                user.getStores() != null ? user.getStores().size() : 0);

        // 店長は1つの店舗にのみ所属する想定
        if (user.getStores() == null || user.getStores().isEmpty()) {
            log.error("店長ユーザーに店舗が割り当てられていません: {}", email);
            throw new RuntimeException("店長ユーザーに店舗が割り当てられていません: " + email);
        }

        // 最初の店舗IDを使用（店長は1つの店舗にのみ所属する想定）
        Store store = user.getStores().iterator().next();
        UUID storeId = store.getId();
        String redirectUrl = "/manager/" + storeId + "/lessons";
        
        log.info("店長のリダイレクトURL取得成功: ユーザー={}, 店舗ID={}, リダイレクト先={}", 
                email, storeId, redirectUrl);
        
        return redirectUrl;
    }
}

