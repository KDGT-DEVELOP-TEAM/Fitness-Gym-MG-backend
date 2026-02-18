package com.example.fitnessgym_mg.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate設定クラス
 * 
 * <p>RestTemplateのBean定義とタイムアウト設定を担当します。</p>
 * 
 * <p>タイムアウト設定:</p>
 * <ul>
     *   <li>接続タイムアウト: デフォルト10秒（環境変数REST_TEMPLATE_CONNECT_TIMEOUT_SECONDSで変更可能）</li>
     *   <li>読み取りタイムアウト: デフォルト30秒（環境変数REST_TEMPLATE_READ_TIMEOUT_SECONDSで変更可能）</li>
     * </ul>
 * 
 * <p>注意: タイムアウト設定は、Supabase Storage APIの応答時間を考慮して調整してください。</p>
 */
@Configuration
public class RestTemplateConfig {
    
    @Value("${rest.template.connect-timeout-seconds:10}")
    private int connectTimeoutSeconds;
    
    @Value("${rest.template.read-timeout-seconds:30}")
    private int readTimeoutSeconds;
    
    /**
     * RestTemplate Bean定義
     * 
     * <p>タイムアウト設定:</p>
     * <ul>
     *   <li>接続タイムアウト: デフォルト10秒（環境変数で変更可能）</li>
     *   <li>読み取りタイムアウト: デフォルト30秒（環境変数で変更可能）</li>
     * </ul>
     * 
     * @param builder RestTemplateBuilder（Spring Bootが自動注入）
     * @return 設定済みのRestTemplateインスタンス
     */
    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .readTimeout(Duration.ofSeconds(readTimeoutSeconds))
                .build();
    }
}

