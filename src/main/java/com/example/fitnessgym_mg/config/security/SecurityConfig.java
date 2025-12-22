package com.example.fitnessgym_mg.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.fitnessgym_mg.config.security.handler.CustomAuthenticationSuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomAuthenticationSuccessHandler successHandler;

	@Bean
	public PasswordEncoder passwordEncoder() {
		// ストレングス12で強力な暗号化（デフォルトは10）
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				// 認可設定
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/login", "/error", "/css/**", "/js/**", "/images/**").permitAll()
						.requestMatchers("/api/auth/**").permitAll() // ログイン・認証系のみ公開
						.requestMatchers("/api/admin/**").hasRole("ADMIN") // 管理者専用API
						.requestMatchers("/api/**").authenticated() // それ以外の API は認証必須
						.anyRequest().authenticated())

				// ログイン設定
				.formLogin(form -> form
						.loginPage("/login")
						.usernameParameter("email")
						.passwordParameter("password")
						.successHandler(successHandler)
						.failureUrl("/login?error=true"))

				// ログアウト設定
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login?logout=true")
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID"))

				// セッション管理設定
				.sessionManagement(session -> session
						.sessionFixation().migrateSession() // セッション固定攻撃対策
						.maximumSessions(1) // 同時セッション数制限
						.maxSessionsPreventsLogin(false) // 新しいセッションを優先
				)

				// セキュリティヘッダー設定
				.headers(headers -> headers
						.contentSecurityPolicy(csp -> csp
								.policyDirectives(
										"default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:"))
						.frameOptions(frame -> frame.deny()) // Clickjacking対策
						.xssProtection(xss -> xss.headerValue("1; mode=block")) // XSS対策
						.contentTypeOptions(options -> options.disable()) // MIME sniffing対策（デフォルトで有効）
				)

				// CSRF 設定 — HTML 画面系は CSRF 有効のまま
				.csrf(csrf -> csrf
						.ignoringRequestMatchers("/api/**") // API のみ CSRF 無効
				);

		return http.build();
	}
}