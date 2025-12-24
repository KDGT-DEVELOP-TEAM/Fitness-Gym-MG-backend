package com.example.fitnessgym_mg.util;

import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

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

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    /**
     * 秘密鍵を取得
     * HMAC-SHA256アルゴリズム用の秘密鍵を生成
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * ユーザー情報からJWTトークンを生成
     * 
     * @param user ユーザーエンティティ
     * @return JWTトークン文字列
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
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
     * トークンの有効性を検証
     * 
     * @param token JWTトークン
     * @return 有効な場合true、無効な場合false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * トークンからClaimsを取得
     * 
     * @param token JWTトークン
     * @return Claims
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * トークンからユーザーIDを取得
     * 
     * @param token JWTトークン
     * @return ユーザーID（UUID）
     */
    public UUID getUserIdFromToken(String token) {
        String subject = getClaims(token).getSubject();
        return UUID.fromString(subject);
    }

    /**
     * トークンからメールアドレスを取得
     * 
     * @param token JWTトークン
     * @return メールアドレス
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).get("email", String.class);
    }

    /**
     * トークンからロールを取得
     * 
     * @param token JWTトークン
     * @return ロール文字列（ADMIN, MANAGER, TRAINER）
     */
    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * トークンからユーザー名を取得
     * 
     * @param token JWTトークン
     * @return ユーザー名
     */
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
}
