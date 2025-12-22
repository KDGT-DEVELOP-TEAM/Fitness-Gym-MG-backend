package com.example.fitnessgym_mg.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * バッチ署名付きURLレスポンスDTO
 * 複数の画像に対する署名付きURLを一括で返す際に使用
 */
@Data
@AllArgsConstructor
public class BatchSignedUrlResponse {
    
    /**
     * 画像IDと署名付きURLのリスト
     */
    private List<ImageSignedUrl> urls;
    
    /**
     * 画像IDと署名付きURLのペア
     */
    @Data
    @AllArgsConstructor
    public static class ImageSignedUrl {
        /**
         * 画像ID
         */
        private UUID imageId;
        
        /**
         * 署名付きURL
         */
        private String signedUrl;
        
        /**
         * 有効期限
         */
        private OffsetDateTime expiresAt;
    }
}
