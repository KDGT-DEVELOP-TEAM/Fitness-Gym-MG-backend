package com.example.fitnessgym_mg.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lombok.extern.slf4j.Slf4j;

/**
 * メールアドレスハッシュ化ユーティリティ
 * 
 * <p>個人情報保護のため、ログにはメールアドレスを直接出力せず、ハッシュ値を出力する。</p>
 * <p>SHA-256アルゴリズムを使用してメールアドレスをハッシュ化します。</p>
 */
@Slf4j
public final class EmailHashUtil {

	private EmailHashUtil() {
		// インスタンス化を防ぐ
	}

	/**
	 * メールアドレスをSHA-256ハッシュ化（ログ出力用）
	 * 
	 * <p>個人情報保護のため、ログにはメールアドレスを直接出力せず、ハッシュ値を出力する。</p>
	 * 
	 * @param email メールアドレス
	 * @return SHA-256ハッシュ値（16進数文字列）
	 */
	public static String hashEmail(String email) {
		if (email == null) {
			return "null";
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(email.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			// SHA-256は標準アルゴリズムなので、この例外は発生しないはず
			log.error("SHA-256 algorithm not found", e);
			return "hash_error";
		}
	}
}
