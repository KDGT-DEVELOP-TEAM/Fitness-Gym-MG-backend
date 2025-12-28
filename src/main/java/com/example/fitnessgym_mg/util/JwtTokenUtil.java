package com.example.fitnessgym_mg.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.fitnessgym_mg.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

/**
 * JWTトークン生成・検証ユーティリティクラス
 * 
 * <p>JWTトークンの生成、検証、情報抽出を行います。
 * フロントエンドとのBearer認証に使用されます。</p>
 * 
 * <p><b>セキュリティ設計:</b></p>
 * <ul>
 *   <li>秘密鍵はbyte[]で管理し、String参照を早期にnull化</li>
 *   <li>アプリケーション終了時に秘密鍵をゼロクリア（@PreDestroy）</li>
 *   <li>アルゴリズムを明示的に指定（HS256）</li>
 *   <li>Audience検証はOR条件で実装（設定変更時の障害を防止）</li>
 *   <li>JWT Claimsには最小限の情報のみを含める（個人情報はDBから取得）</li>
 * </ul>
 * 
 * <p><b>Audience検証:</b>
 * OR条件で検証します。トークンのaudienceが設定されたaudienceのいずれかと一致すれば有効です。
 * これにより、複数のクライアント（frontend、admin、mobileなど）を柔軟にサポートできます。</p>
 */
@Slf4j
@Component
public class JwtTokenUtil {

	private static final String JWT_ISSUER = "fitnessgym-mg";

	@Value("${jwt.secret}")
	private String secretKey;

	/**
	 * JWT秘密鍵（byte配列形式）
	 * セキュリティベストプラクティスに従い、Stringではなくbyte[]で管理
	 */
	private byte[] secretKeyBytes;

	/**
	 * JWTトークンの有効期限（ミリ秒）
	 */
	@Value("${jwt.expiration}")
	private long expirationTimeMs;

	/**
	 * JWTトークンの対象（audience）
	 * 設定ファイルではカンマ区切りで複数指定可能（例: "frontend,admin"）
	 * 既存の単一値設定（"frontend"）とも互換性あり
	 */
	@Value("${jwt.audience:frontend}")
	private String jwtAudience;

	/**
	 * JWTトークンの対象（audience）セット
	 * 起動時にStringからSetに変換される
	 * セットを使用することで重複を排除し、contains性能を向上（O(1)）
	 */
	private Set<String> jwtAudiences;

	/**
	 * 起動時にJWT秘密鍵をbyte[]に変換し、長さを検証
	 * また、audienceをStringからSetに変換
	 * 
	 * <p>HS256では最低256bit (32byte)推奨です。</p>
	 * 
	 * <p><b>セキュリティ対策:</b></p>
	 * <ul>
	 *   <li>Stringからbyte[]に変換後、String参照をnull化（GCを促進）</li>
	 *   <li>audienceを不変Setに変換（重複排除とパフォーマンス向上）</li>
	 *   <li>audienceが空でないことを検証</li>
	 * </ul>
	 * 
	 * @throws IllegalStateException 秘密鍵が設定されていない、または長さが不足している場合
	 * @throws IllegalStateException audienceが空の場合
	 */
	@PostConstruct
	private void validateSecretKey() {
		if (secretKey == null) {
			throw new IllegalStateException("JWT secret key is not configured");
		}

		// Stringからbyte[]に変換
		secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

		// 長さを検証
		int byteLength = secretKeyBytes.length;
		if (byteLength < 32) {
			throw new IllegalStateException(
					"JWT secret key is too short. Minimum 32 bytes (256 bits) required for HS256. " +
							"Current length: " + byteLength + " bytes");
		}

		// セキュリティ対策: String参照をnull化（GCを促進）
		secretKey = null;

		// audienceをStringからSetに変換（カンマ区切り対応）
		// 不変Setを使用してセキュリティを強化
		if (jwtAudience != null && !jwtAudience.trim().isEmpty()) {
			jwtAudiences = Arrays.stream(jwtAudience.split(","))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.collect(Collectors.toUnmodifiableSet());
		} else {
			jwtAudiences = Set.of("frontend"); // デフォルト値
		}

		// audienceが空でないことを確認
		if (jwtAudiences.isEmpty()) {
			throw new IllegalStateException("JWT audience must not be empty");
		}

		// audienceのString参照をnull化
		jwtAudience = null;

		log.info("JWT secret key length validated: {} bytes", byteLength);
		log.info("JWT audiences configured: {}", jwtAudiences);
	}

	/**
	 * アプリケーション終了時にJWT秘密鍵をメモリからゼロクリア
	 * 
	 * <p>セキュリティベストプラクティスに従い、秘密鍵のbyte配列を明示的にゼロクリアします。
	 * これにより、メモリダンプ攻撃に対する耐性を向上させます。</p>
	 * 
	 * <p>注意: Springのライフサイクル管理下で実行されるため、
	 * 正常終了時のみ呼び出されます。強制終了時は実行されません。</p>
	 */
	@PreDestroy
	void destroy() {
		if (secretKeyBytes != null) {
			Arrays.fill(secretKeyBytes, (byte) 0);
			secretKeyBytes = null;
			log.debug("JWT secret key bytes cleared from memory");
		}
	}

	/**
	 * 秘密鍵を取得
	 * 
	 * <p>HMAC-SHA256アルゴリズム用の秘密鍵を生成します。
	 * byte[]からSecretKeyオブジェクトを生成します。</p>
	 * 
	 * @return HMAC-SHA256用のSecretKey
	 * @throws IllegalStateException secretKeyBytesが初期化されていない場合
	 */
	private SecretKey getSigningKey() {
		if (secretKeyBytes == null) {
			throw new IllegalStateException("JWT secret key bytes not initialized. Check @PostConstruct execution.");
		}
		return Keys.hmacShaKeyFor(secretKeyBytes);
	}

	/**
	 * ユーザー情報からJWTトークンを生成
	 * 
	 * <p><b>JWT Claims:</b></p>
	 * <ul>
	 *   <li>iss (issuer): "fitnessgym-mg"</li>
	 *   <li>aud (audience): 設定されたaudienceセット</li>
	 *   <li>sub (subject): ユーザーID（UUID）</li>
	 *   <li>role: ユーザーロール（ADMIN, MANAGER, TRAINER）</li>
	 *   <li>iat (issued at): 発行日時</li>
	 *   <li>exp (expiration): 有効期限</li>
	 * </ul>
	 * 
	 * <p><b>セキュリティ考慮事項:</b>
	 * 個人情報（email、name）はJWTに含めず、DBから取得する設計です。
	 * これにより、JWT漏洩時の個人情報漏洩リスクを低減します。</p>
	 * 
	 * @param user ユーザーエンティティ
	 * @return JWTトークン文字列
	 */
	public String generateToken(User user) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expirationTimeMs);

		return Jwts.builder()
				.issuer(JWT_ISSUER)
				.audience().add(jwtAudiences).and()
				.subject(user.getId().toString())
				.claim("role", user.getRole().name())
				.issuedAt(now)
				.expiration(expiryDate)
				.signWith(getSigningKey(), Jwts.SIG.HS256)
				.compact();
	}

	/**
	 * トークンをパースして検証し、Claimsを返す
	 * 
	 * <p>同一リクエスト内で複数回パースすることを避けるため、
	 * このメソッドで1回だけパースし、取得したClaimsを再利用する。</p>
	 * 
	 * <p><b>Audience検証:</b> OR条件で検証します。
	 * jjwtの`requireAudience(Collection)`はAND条件（すべての要素を含む必要がある）のため、
	 * 手動でOR条件チェックを実装しています。
	 * トークンのaudienceが設定されたaudienceのいずれかと一致すれば有効とします。</p>
	 * 
	 * @param token JWTトークン
	 * @return Claims
	 * @throws ExpiredJwtException トークンが期限切れの場合
	 * @throws SignatureException 署名が無効な場合
	 * @throws MalformedJwtException トークンの形式が不正な場合
	 * @throws UnsupportedJwtException サポートされていないトークンの場合
	 * @throws IllegalArgumentException クレームが空の場合
	 * @throws SecurityException audience検証に失敗した場合
	 */
	public Claims parseAndValidate(String token) {
		// まず署名とissuerを検証（audienceは後で手動検証）
		Claims claims = Jwts.parser()
				.verifyWith(getSigningKey())
				.requireIssuer(JWT_ISSUER)
				.build()
				.parseSignedClaims(token)
				.getPayload();

		// OR条件でaudienceを検証
		// トークンのaudienceがnull、または設定されたaudienceと共通要素がない場合はエラー
		Set<String> tokenAudience = claims.getAudience();
		if (tokenAudience == null || Collections.disjoint(tokenAudience, jwtAudiences)) {
			throw new SecurityException(
					"JWT audience validation failed. Token audience: " + tokenAudience +
							", Required (any of): " + jwtAudiences);
		}

		return claims;
	}

	/**
	 * トークンの有効性を検証
	 * 
	 * 検証に失敗した場合は例外を投げる。
	 * 呼び出し元で例外の種類（ExpiredJwtException、SignatureException など）を
	 * 区別して処理できるようにする。
	 * 
	 * @param token JWTトークン
	 * @throws ExpiredJwtException トークンが期限切れの場合
	 * @throws SignatureException 署名が無効な場合
	 * @throws MalformedJwtException トークンの形式が不正な場合
	 * @throws UnsupportedJwtException サポートされていないトークンの場合
	 * @throws IllegalArgumentException クレームが空の場合
	 */
	public void validateToken(String token) {
		parseAndValidate(token);
	}

	/**
	 * トークンからClaimsを取得
	 * 
	 * @param token JWTトークン
	 * @return Claims
	 * @deprecated このメソッドは非推奨です。代わりに {@link #parseAndValidate(String)} を使用してください。
	 */
	@Deprecated
	private Claims getClaims(String token) {
		return parseAndValidate(token);
	}

	// ===== Claims操作ユーティリティ（再パースを避けるため） =====

	/**
	 * Claimsからロールを取得
	 * 
	 * @param claims JWT Claims
	 * @return ロール文字列（ADMIN, MANAGER, TRAINER）
	 */
	public String getRoleFromClaims(Claims claims) {
		return claims.get("role", String.class);
	}

	/**
	 * ClaimsからユーザーIDを取得
	 * 
	 * @param claims JWT Claims
	 * @return ユーザーID（UUID）
	 */
	public UUID getUserIdFromClaims(Claims claims) {
		String subject = claims.getSubject();
		return UUID.fromString(subject);
	}

	// ===== 後方互換性のためのメソッド（非推奨） =====

	/**
	 * トークンからユーザーIDを取得
	 * 
	 * @param token JWTトークン
	 * @return ユーザーID（UUID）
	 * @deprecated このメソッドは再パースを引き起こします。{@link #parseAndValidate(String)} で取得したClaimsを {@link #getUserIdFromClaims(Claims)} に渡してください。
	 */
	@Deprecated
	public UUID getUserIdFromToken(String token) {
		String subject = getClaims(token).getSubject();
		return UUID.fromString(subject);
	}

	/**
	 * トークンからロールを取得
	 * 
	 * @param token JWTトークン
	 * @return ロール文字列（ADMIN, MANAGER, TRAINER）
	 * @deprecated このメソッドは再パースを引き起こします。{@link #parseAndValidate(String)} で取得したClaimsを {@link #getRoleFromClaims(Claims)} に渡してください。
	 */
	@Deprecated
	public String getRoleFromToken(String token) {
		return getClaims(token).get("role", String.class);
	}

	/**
	 * トークンの有効期限を取得
	 * 
	 * @param token JWTトークン
	 * @return 有効期限
	 */
	public Date getExpirationFromToken(String token) {
		return getClaims(token).getExpiration();
	}

	/**
	 * トークンが期限切れかどうかを確認
	 * 
	 * @param token JWTトークン
	 * @return 期限切れの場合true
	 */
	public boolean isTokenExpired(String token) {
		try {
			Date expiration = getExpirationFromToken(token);
			return expiration.before(new Date());
		} catch (ExpiredJwtException e) {
			return true;
		}
	}

	/**
	 * JWTトークンの有効期限（ミリ秒）を取得
	 * CookieのMaxAge設定などに使用
	 * 
	 * @return 有効期限（ミリ秒）
	 */
	public long getExpirationTimeMs() {
		return expirationTimeMs;
	}
}
