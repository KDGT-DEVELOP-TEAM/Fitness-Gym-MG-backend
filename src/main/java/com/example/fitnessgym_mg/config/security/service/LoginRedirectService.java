package com.example.fitnessgym_mg.config.security.service;

import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LoginRedirectService {

	// アプリ内で許可するリダイレクト先のみを定義
	private static final Set<String> ALLOWED_PATHS = Set.of(
			"/",
			"/dashboard",
			"/admin",
			"/user/home");

	/**
	 * ログイン成功後のリダイレクト先を決定する
	 * 外部URLや想定外のパスはすべてデフォルトにフォールバックする
	 */
	public String resolveRedirectUrl(String email, Authentication authentication) {

		// ここでは例として固定値（実際は権限等で分岐してもよい）
		String redirectUrl = determineRedirectUrl(authentication);

		// ===== 安全性チェック =====

		// null / 空文字チェック
		if (redirectUrl == null || redirectUrl.isBlank()) {
			log.warn("リダイレクトURLが不正なためデフォルトへフォールバック: email={}", email);
			return "/";
		}

		// 外部URL防止（http / https）
		if (redirectUrl.startsWith("http://") || redirectUrl.startsWith("https://")) {
			log.warn("外部URLへのリダイレクトを検知したため拒否: {}", redirectUrl);
			return "/";
		}

		// 内部パス以外を拒否
		if (!redirectUrl.startsWith("/")) {
			log.warn("内部パス以外のリダイレクトを検知したため拒否: {}", redirectUrl);
			return "/";
		}

		// ホワイトリストチェック
		if (!ALLOWED_PATHS.contains(redirectUrl)) {
			log.warn("許可されていないリダイレクト先のため拒否: {}", redirectUrl);
			return "/";
		}

		return redirectUrl;
	}

	/**
	 * 認証情報からリダイレクト先を決定する
	 * （権限別分岐などはここで行う）
	 */
	private String determineRedirectUrl(Authentication authentication) {

		// 例：ROLE_ADMIN を持つ場合
		if (authentication.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
			return "/admin";
		}

		// それ以外
		return "/dashboard";
	}
}
