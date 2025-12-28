package com.example.fitnessgym_mg.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * バッチ署名付きURLレスポンスDTO
 * 複数の画像に対する署名付きURLを一括で返す際に使用
 * 
 * <p>セキュリティ: 署名付きURLは認可情報を含むため、ログ出力から除外します。</p>
 */
@Data
@ToString(exclude = "urls") // セキュリティ: 署名付きURLリストをログに出力しない
@AllArgsConstructor
public class BatchSignedUrlResponse {
    
    /**
     * 画像IDと署名付きURLのリスト
     * 
     * <p>API安定性のため、デフォルト値として空リストを設定しています。</p>
     * <p>Service層では必ず空リスト以上の値を返しますが、nullの可能性を排除することでAPI仕様を安定させます。</p>
     */
    private List<ImageSignedUrl> urls = List.of();
    
    /**
     * 画像IDと署名付きURLのペア
     * 
     * <p>セキュリティ: 署名付きURLは認可情報を含むため、ログ出力から除外します。</p>
     */
    @Data
    @ToString(exclude = "signedUrl") // セキュリティ: 署名付きURLをログに出力しない
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
