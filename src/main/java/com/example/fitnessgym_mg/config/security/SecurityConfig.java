package com.example.fitnessgym_mg.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security設定クラス
 * JWT認証を使用したステートレスなREST API用セキュリティ設定
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	/**
	 * BCryptパスワードエンコーダーのストレングス
	 * デフォルトは10だが、セキュリティ強化のため12に設定
	 */
	private static final int BCRYPT_STRENGTH = 12;

	/**
	 * Content Security Policy (CSP)
	 * XSS攻撃を防ぐためのセキュリティヘッダー
	 */
	private static final String CSP_POLICY = 
			"default-src 'self'; " +
			"script-src 'self' 'unsafe-inline'; " +
			"style-src 'self' 'unsafe-inline'; " +
			"img-src 'self' data: https:; " +
			"font-src 'self' data:";

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				// 認可設定（REST APIのみ）
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/**").permitAll() // ログイン・認証系のみ公開
						.requestMatchers("/api/admin/**").hasRole("ADMIN") // 管理者専用API
						.requestMatchers("/api/**").authenticated() // それ以外の API は認証必須
						.anyRequest().denyAll()) // APIエンドポイント以外は拒否

				// フォームログインを無効化（JWT認証のみ使用）
				.formLogin(form -> form.disable())

				// ログアウト設定（JWT認証ではサーバー側での処理は最小限）
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.logoutSuccessHandler((request, response, authentication) -> {
							// JWTはステートレスなので、サーバー側では特に処理しない
							// クライアント側でトークンを削除する
							response.setStatus(200);
						}))

				// セッション管理設定（JWT認証のためステートレスに設定）
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// セキュリティヘッダー設定
				.headers(headers -> headers
						.contentSecurityPolicy(csp -> csp.policyDirectives(CSP_POLICY))
						.frameOptions(frame -> frame.deny()) // Clickjacking対策
						.xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)) // XSS対策
						.contentTypeOptions(options -> options.disable()) // MIME sniffing対策（デフォルトで有効）
				)

				// CSRF無効化（JWT認証ではCSRFトークンは不要）
				.csrf(csrf -> csrf.disable())

				// JWT認証フィルターを追加
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
