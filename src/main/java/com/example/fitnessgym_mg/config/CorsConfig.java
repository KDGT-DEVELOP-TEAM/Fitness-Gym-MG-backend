package com.example.fitnessgym_mg.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * CORS設定クラス
 * 
 * <p>フロントエンドからのリクエストを許可するためのCORS設定を提供します。</p>
 * <p>環境変数`CORS_ALLOWED_ORIGINS`で許可するオリジンを設定できます（カンマ区切りで複数指定可能）。</p>
 * 
 * <p>セキュリティ注意事項:</p>
 * <ul>
 *   <li>本番環境では、開発環境のデフォルト値（localhost等）が残らないように注意してください</li>
 *   <li>環境変数CORS_ALLOWED_ORIGINSを必ず設定してください</li>
 * </ul>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CorsConfig {
    
    private final Environment environment;
    
    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;
    
    /**
     * CORS設定を提供するBean
     * 
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 許可するオリジンを設定（環境変数から取得、カンマ区切り対応）
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .collect(Collectors.toList());
        
        // 空のリストチェック
        if (origins.isEmpty()) {
            throw new IllegalStateException(
                "CORS設定エラー: 許可するオリジンが設定されていません。環境変数CORS_ALLOWED_ORIGINSを確認してください。");
        }
        
        // 本番環境での検証（開発環境のデフォルト値が残っていないかチェック）
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isProduction = Arrays.stream(activeProfiles)
            .anyMatch(profile -> profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"));
        
        if (isProduction) {
            // 本番環境でlocalhostや127.0.0.1が含まれている場合は例外をスローして起動を阻止
            boolean hasLocalhost = origins.stream()
                .anyMatch(origin -> origin.contains("localhost") || origin.contains("127.0.0.1"));
            
            if (hasLocalhost) {
                log.error("CORS設定エラー: 本番環境でlocalhostや127.0.0.1が許可されています。");
                log.error("許可されているオリジン: {}", origins);
                throw new IllegalStateException(
                    "本番環境ではlocalhostや127.0.0.1を許可できません。環境変数CORS_ALLOWED_ORIGINSを確認してください。");
            }
            
            // HTTPSでないオリジンが含まれている場合は例外をスローして起動を阻止
            boolean hasHttp = origins.stream()
                .anyMatch(origin -> origin.startsWith("http://") && !origin.startsWith("https://"));
            
            if (hasHttp) {
                log.error("CORS設定エラー: 本番環境でHTTP（非HTTPS）のオリジンが許可されています。");
                throw new IllegalStateException(
                    "本番環境ではHTTP（非HTTPS）のオリジンを許可できません。環境変数CORS_ALLOWED_ORIGINSを確認してください。");
            }
        }
        
        configuration.setAllowedOrigins(origins);
        
        // 許可するHTTPメソッド
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // 許可するヘッダー（必要なヘッダーのみを明示的に許可）
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // 認証情報（Cookie、Authorizationヘッダー）を許可
        configuration.setAllowCredentials(true);
        
        // プリフライトリクエストのキャッシュ時間（秒）
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}

