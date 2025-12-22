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
		log.debug("ログイン成功: {}", email); // DEBUGレベルに変更（セキュリティ考慮）

		String redirectUrl = redirectService.resolveRedirectUrl(authentication);
		log.debug("リダイレクト先: {}", redirectUrl);

		try {
			response.sendRedirect(redirectUrl);
		} catch (IOException e) {
			log.error("リダイレクト失敗: email={}, url={}", email, redirectUrl, e);
			// フォールバック：エラーが発生した場合はルートへリダイレクト
			response.sendRedirect("/");
		}
	}
}