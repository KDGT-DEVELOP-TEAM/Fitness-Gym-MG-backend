package com.example.fitnessgym_mg.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "supabase.storage")
public class SupabaseStorageProperties {
    private String url;           // SUPABASE_STORAGE_URL
    private String serviceKey;    // SUPABASE_SERVICE_KEY
    private String bucket = "postures"; // デフォルト値
    private int maxFileSizeMb = 10; // 最大ファイルサイズ（MB）
}
