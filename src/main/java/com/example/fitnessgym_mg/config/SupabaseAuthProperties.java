package com.example.fitnessgym_mg.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Supabase Auth設定プロパティ
 * application.propertiesのsupabase.auth.*プロパティをバインド
 * 
 * 設定例:
 * <pre>
 * supabase.auth.url=${SUPABASE_AUTH_URL}
 * supabase.auth.service-key=${SUPABASE_AUTH_SERVICE_KEY}
 * </pre>
 */
@Data
@ToString(exclude = "serviceKey")
@Validated
@Configuration
@ConfigurationProperties(prefix = "supabase.auth")
public class SupabaseAuthProperties {
    
    /**
     * Supabase Auth API URL
     * 環境変数: SUPABASE_AUTH_URL
     * 例: https://xxxxx.supabase.co
     */
    @NotBlank
    private String url;
    
    /**
     * Supabase Service Role Key（機密情報）
     * 環境変数: SUPABASE_AUTH_SERVICE_KEY
     * 注意: この値はログに出力しないこと
     */
    @NotBlank
    private String serviceKey;
}
