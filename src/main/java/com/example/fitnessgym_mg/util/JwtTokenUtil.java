package com.example.fitnessgym_mg.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;

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
 * JWTトークンの生成、検証、情報抽出を行います。
 * フロントエンドとのBearer認証に使用されます。
 */
@Slf4j
@Component
public class JwtTokenUtil {

    private static final String JWT_ISSUER = "fitnessgym-mg";

    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * JWTトークンの有効期限（ミリ秒）
     */
    @Value("${jwt.expiration}")
    private long expirationTimeMs;

    /**
     * JWTトークンの対象（audience）
     */
    @Value("${jwt.audience:frontend}")
    private String jwtAudience;

    /**
     * 起動時にJWT秘密鍵の長さを検証
     * HS256では最低256bit (32byte)推奨
     */
    @PostConstruct
    private void validateSecretKey() {
        int byteLength = secretKey != null ? secretKey.getBytes(StandardCharsets.UTF_8).length : 0;
        if (secretKey == null || byteLength < 32) {
            throw new IllegalStateException(
                    "JWT secret key is too short. Minimum 32 bytes (256 bits) required for HS256. " +
                    "Current length: " + byteLength + " bytes"
            );
        }
        log.info("JWT secret key length validated: {} bytes", byteLength);
    }

    /**
     * 秘密鍵を取得
     * HMAC-SHA256アルゴリズム用の秘密鍵を生成
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * ユーザー情報からJWTトークンを生成
     * 
     * @param user ユーザーエンティティ
     * @return JWTトークン文字列
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTimeMs);

        return Jwts.builder()
                .issuer(JWT_ISSUER)
                .audience().add(jwtAudience).and()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * トークンをパースして検証し、Claimsを返す
     * 
     * 同一リクエスト内で複数回パースすることを避けるため、
     * このメソッドで1回だけパースし、取得したClaimsを再利用する。
     * 
     * @param token JWTトークン
     * @return Claims
     * @throws ExpiredJwtException トークンが期限切れの場合
     * @throws SignatureException 署名が無効な場合
     * @throws MalformedJwtException トークンの形式が不正な場合
     * @throws UnsupportedJwtException サポートされていないトークンの場合
     * @throws IllegalArgumentException クレームが空の場合
     */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(JWT_ISSUER)
                .requireAudience(jwtAudience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
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
     * Claimsからメールアドレスを取得
     * 
     * @param claims JWT Claims
     * @return メールアドレス
     */
    public String getEmailFromClaims(Claims claims) {
        return claims.get("email", String.class);
    }

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

    /**
     * Claimsからユーザー名を取得
     * 
     * @param claims JWT Claims
     * @return ユーザー名
     */
    public String getNameFromClaims(Claims claims) {
        return claims.get("name", String.class);
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
     * トークンからメールアドレスを取得
     * 
     * @param token JWTトークン
     * @return メールアドレス
     * @deprecated このメソッドは再パースを引き起こします。{@link #parseAndValidate(String)} で取得したClaimsを {@link #getEmailFromClaims(Claims)} に渡してください。
     */
    @Deprecated
    public String getEmailFromToken(String token) {
        return getClaims(token).get("email", String.class);
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
     * トークンからユーザー名を取得
     * 
     * @param token JWTトークン
     * @return ユーザー名
     * @deprecated このメソッドは再パースを引き起こします。{@link #parseAndValidate(String)} で取得したClaimsを {@link #getNameFromClaims(Claims)} に渡してください。
     */
    @Deprecated
    public String getNameFromToken(String token) {
        return getClaims(token).get("name", String.class);
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
