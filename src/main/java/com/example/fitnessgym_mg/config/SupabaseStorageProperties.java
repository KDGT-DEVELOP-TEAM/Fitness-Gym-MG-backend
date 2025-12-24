package com.example.fitnessgym_mg.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Supabase Storage設定プロパティ
 * application.propertiesのsupabase.storage.*プロパティをバインド
 * 
 * 設定例:
 * <pre>
 * supabase.storage.url=${SUPABASE_STORAGE_URL}
 * supabase.storage.service-key=${SUPABASE_SERVICE_KEY}
 * supabase.storage.bucket=${SUPABASE_BUCKET_NAME:postures}
 * supabase.storage.max-file-size-mb=${SUPABASE_MAX_FILE_SIZE_MB:10}
 * </pre>
 */
@Data
@ToString(exclude = "serviceKey")
@Validated
@Configuration
@ConfigurationProperties(prefix = "supabase.storage")
public class SupabaseStorageProperties {
    
    /**
     * Supabase Storage API URL
     * 環境変数: SUPABASE_STORAGE_URL
     * 例: https://xxxxx.supabase.co
     */
    @NotBlank
    private String url;
    
    /**
     * Supabase Service Key（機密情報）
     * 環境変数: SUPABASE_SERVICE_KEY
     * 注意: この値はログに出力しないこと
     */
    @NotBlank
    private String serviceKey;
    
    /**
     * Storageバケット名
     * デフォルト値: postures
     * 環境変数: SUPABASE_BUCKET_NAME
     */
    private String bucket = "postures";
    
    /**
     * 最大ファイルサイズ（MB）
     * デフォルト値: 10MB
     * 環境変数: SUPABASE_MAX_FILE_SIZE_MB
     */
    private int maxFileSizeMb = 10;
}
