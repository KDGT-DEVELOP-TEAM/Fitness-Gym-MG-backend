package com.example.fitnessgym_mg.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS設定クラス
 * 
 * <p>フロントエンドからのリクエストを許可するためのCORS設定を提供します。</p>
 * <p>環境変数`CORS_ALLOWED_ORIGINS`で許可するオリジンを設定できます（カンマ区切りで複数指定可能）。</p>
 */
@Configuration
public class CorsConfig {
    
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

