package com.example.fitnessgym_mg.config.security.handler;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.fitnessgym_mg.config.security.service.LoginRedirectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final LoginRedirectService redirectService;

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication) throws IOException {

		String email = authentication.getName();
		log.info("ログイン成功: {}", email);

		String redirectUrl = redirectService.resolveRedirectUrl(email, authentication);

		log.info("リダイレクト先: {}", redirectUrl);
		response.sendRedirect(redirectUrl);
	}
}