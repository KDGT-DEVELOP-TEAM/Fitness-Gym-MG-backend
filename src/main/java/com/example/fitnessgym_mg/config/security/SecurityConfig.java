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
		return new BCryptPasswordEncoder();
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

				// CSRF 設定 — HTML 画面系は CSRF 有効のまま
				.csrf(csrf -> csrf
						.ignoringRequestMatchers("/api/**") // API のみ CSRF 無効
				);

		return http.build();
	}
}