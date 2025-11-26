package com.example.fitnessgym_mg.config;

import java.io.IOException;

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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * パスワードエンコーダーのBean定義
     * パスワードをBCryptでハッシュ化するためのエンコーダーを返します
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 認証マネージャーのBean定義
     * ログイン処理を実行するためのマネージャーを返します
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
            // CSRF対策：APIエンドポイントはCSRFチェックを無視
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }

    /**
     * ログイン成功後の権限ベースリダイレクトハンドラー
     * 
     * 処理の流れ：
     * 1. ユーザーの権限を取得
     * 2. 権限に応じてリダイレクト先を決定
     *    - ROLE_ADMIN → /admin/dashboard
     *    - ROLE_MANAGER → /manager/dashboard
     *    - ROLE_TRAINER → /trainer/customers
     * 3. 決定したURLにリダイレクト
     */
    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                    Authentication authentication) throws IOException, ServletException {
                
                String redirectUrl = "/";
                
                // 権限に応じてリダイレクト先を決定
                if (authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"))) {
                    redirectUrl = "/admin/dashboard";
                } else if (authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_MANAGER"))) {
                    redirectUrl = "/manager/dashboard";
                } else if (authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_TRAINER"))) {
                    redirectUrl = "/trainer/customers";
                }
                
                response.sendRedirect(redirectUrl);
            }
        };
    }
}

