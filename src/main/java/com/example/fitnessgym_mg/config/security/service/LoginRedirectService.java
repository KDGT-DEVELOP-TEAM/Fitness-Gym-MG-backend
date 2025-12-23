package com.example.fitnessgym_mg.config.security.service;

import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LoginRedirectService {

	// ロール定数
	private static final String ROLE_ADMIN = "ROLE_ADMIN";
	private static final String ROLE_TRAINER = "ROLE_TRAINER";

	// デフォルトのリダイレクト先
	private static final String DEFAULT_REDIRECT_PATH = "/";

	// アプリ内で許可するリダイレクト先のみを定義
	private static final Set<String> ALLOWED_PATHS = Set.of(
			"/",
			"/dashboard",
			"/admin",
			"/trainer/customers",
			"/user/home");

	/**
	 * ログイン成功後のリダイレクト先を決定する
	 * 外部URLや想定外のパスはすべてデフォルトにフォールバックする
	 */
	public String resolveRedirectUrl(Authentication authentication) {
		String redirectUrl = determineRedirectUrl(authentication);

		if (!isValidRedirectUrl(redirectUrl)) {
			String username = authentication.getName();
			log.warn("無効なリダイレクトURLのためデフォルトへフォールバック: user={}, url={}", username, redirectUrl);
			return DEFAULT_REDIRECT_PATH;
		}

		return redirectUrl;
	}

	/**
	 * リダイレクトURLの妥当性を検証
	 */
	private boolean isValidRedirectUrl(String url) {
		// null / 空文字チェック
		if (url == null || url.isBlank()) {
			return false;
		}

		// 外部URL防止（http / https）
		if (url.startsWith("http://") || url.startsWith("https://")) {
			log.warn("外部URLへのリダイレクトを検知したため拒否: {}", url);
			return false;
		}

		// 内部パス以外を拒否
		if (!url.startsWith("/")) {
			log.warn("内部パス以外のリダイレクトを検知したため拒否: {}", url);
			return false;
		}

		// ホワイトリストチェック
		if (!ALLOWED_PATHS.contains(url)) {
			log.warn("許可されていないリダイレクト先のため拒否: {}", url);
			return false;
		}

		return true;
	}

	/**
	 * 認証情報からリダイレクト先を決定する
	 * （権限別分岐などはここで行う）
	 */
	private String determineRedirectUrl(Authentication authentication) {
		// ROLE_ADMIN を持つ場合
		if (hasRole(authentication, ROLE_ADMIN)) {
			return "/admin";
		}

		// ROLE_TRAINER を持つ場合
		if (hasRole(authentication, ROLE_TRAINER)) {
			return "/trainer/customers";
		}

		// それ以外（一般ユーザー）
		return "/user/home";
	}

	/**
	 * 指定されたロールを持っているか確認
	 */
	private boolean hasRole(Authentication authentication, String role) {
		return authentication.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals(role));
	}
}