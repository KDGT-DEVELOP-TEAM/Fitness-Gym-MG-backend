package com.example.fitnessgym_mg.config;

import java.time.Duration;

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
 *   <li>接続タイムアウト: 5秒（Supabase Storage APIへの接続確立までの最大待機時間）</li>
 *   <li>読み取りタイムアウト: 10秒（Supabase Storage APIからの応答待機時間）</li>
 * </ul>
 * 
 * <p>注意: タイムアウト設定は、Supabase Storage APIの応答時間を考慮して調整してください。</p>
 */
@Configuration
public class RestTemplateConfig {
    
    /**
     * RestTemplate Bean定義
     * 
     * <p>タイムアウト設定:</p>
     * <ul>
     *   <li>接続タイムアウト: 5秒</li>
     *   <li>読み取りタイムアウト: 10秒</li>
     * </ul>
     * 
     * @param builder RestTemplateBuilder（Spring Bootが自動注入）
     * @return 設定済みのRestTemplateインスタンス
     */
    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }
}

