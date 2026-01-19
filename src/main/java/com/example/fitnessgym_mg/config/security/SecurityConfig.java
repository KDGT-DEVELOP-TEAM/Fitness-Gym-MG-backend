package com.example.fitnessgym_mg.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;

import com.example.fitnessgym_mg.filter.LoginAttemptFilter;

import lombok.RequiredArgsConstructor;

import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security設定クラス
 * JWT認証を使用したステートレスなREST API用セキュリティ設定
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final LoginRateLimitFilter loginRateLimitFilter;
	private final LoginAttemptFilter loginAttemptFilter;
	private final CorsConfigurationSource corsConfigurationSource;

	/* =========================
	 * 定数定義（将来分離しやすい）
	 * ========================= */
	private static final int BCRYPT_STRENGTH = 12;

	private static final String[] PUBLIC_API = {
			"/api/auth/login",  // POST /api/auth/login のみ認証不要
			"/api/password-reset/request"  // POST /api/password-reset/request は認証不要（パスワードを忘れたユーザーが使用）
	};

	private static final String[] ADMIN_API = {
			"/api/admin/**"
	};

	private static final String CSP_POLICY = 
			"default-src 'self'; " +
			"script-src 'self'; " +
			"style-src 'self'; " +
			"img-src 'self' data: https:; " +
			"font-src 'self' data:";

	/* =========================
	 * Bean定義
	 * ========================= */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
	}

	@Bean
	AuthenticationManager authenticationManager(
			AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	/* =========================
	 * Security Filter
	 * ========================= */
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http
			// REST API前提：CORS有効
			.cors(cors -> cors.configurationSource(corsConfigurationSource))

			// CSRF無効（JWT前提）
			.csrf(csrf -> csrf.disable())

			// セッションは使わない
			.sessionManagement(session ->
					session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			// 認可設定
				.authorizeHttpRequests(auth -> auth
					.requestMatchers(PUBLIC_API).permitAll()
					.requestMatchers(ADMIN_API).hasRole("ADMIN")
					.requestMatchers("/api/**").authenticated()
					.anyRequest().denyAll()
			)

			// フォームログイン・ログアウト無効
				.formLogin(form -> form.disable())
			.logout(logout -> logout.disable())

			// セキュリティヘッダー
				.headers(headers -> headers
					.contentSecurityPolicy(csp ->
							csp.policyDirectives(CSP_POLICY))
					.frameOptions(frame -> frame.deny())
					.contentTypeOptions(contentType -> {}) // X-Content-Type-Options: nosniff
				)

			// LoginAttemptFilter（UsernamePasswordAuthenticationFilterより前に配置）
			.addFilterBefore(
					loginAttemptFilter,
					UsernamePasswordAuthenticationFilter.class
				)

			// レートリミットフィルター（UsernamePasswordAuthenticationFilterより前に配置）
			.addFilterBefore(
					loginRateLimitFilter,
					UsernamePasswordAuthenticationFilter.class
			)

			// JWTフィルター
			.addFilterBefore(
					jwtAuthenticationFilter,
					UsernamePasswordAuthenticationFilter.class
			);

		return http.build();
	}
	
	/**
	 * ForwardedHeaderFilterをBeanとして登録
	 * X-Forwarded-Forヘッダーを適切に処理し、信頼できるプロキシからのIPのみを取得
	 * 
	 * <p>セキュリティ注意事項:</p>
	 * <ul>
	 *   <li>ForwardedHeaderFilterはserver.forward-headers-strategy=frameworkにより自動設定されます</li>
	 *   <li>信頼できるプロキシのIPアドレスを明示的に設定するには、application.propertiesで
	 *       server.tomcat.remoteip.internal-proxiesを設定してください</li>
	 *   <li>本番環境では、環境変数TRUSTED_PROXY_IPSで信頼できるプロキシのIPアドレス（CIDR表記、パイプ区切り）を設定してください</li>
	 *   <li>開発環境では直接接続を想定し、TRUSTED_PROXY_IPSは空にしてください</li>
	 * </ul>
	 * 
	 * <p>設定方法:</p>
	 * <ul>
	 *   <li>application.propertiesでserver.tomcat.remoteip.internal-proxiesを設定することで、
	 *       TomcatのRemoteIpValveが信頼できるプロキシのIPアドレスのみを受け入れるようになります</li>
	 *   <li>信頼できないプロキシからのX-Forwarded-Forヘッダーは無視され、元のIPアドレスが使用されます</li>
	 * </ul>
	 */
	@Bean
	public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
		ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
		FilterRegistrationBean<ForwardedHeaderFilter> registration = 
			new FilterRegistrationBean<>(filter);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}
}
