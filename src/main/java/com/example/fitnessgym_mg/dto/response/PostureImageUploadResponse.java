package com.example.fitnessgym_mg.dto.response;

import com.example.fitnessgym_mg.entity.PostureImage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 姿勢画像アップロードレスポンスDTO
 * 画像アップロード成功時に返される情報を含む
 * 
 * <p>セキュリティ: ストレージキーは機密情報のため、ログ出力から除外します。</p>
 */
@Data
@ToString(exclude = "storageKey") // セキュリティ: ストレージキーをログに出力しない
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureImageUploadResponse {
    
    /**
     * 画像ID
     */
    private UUID id;
    
    /**
     * 姿勢グループID
     */
    private UUID postureGroupId;
    
    /**
     * ストレージキー（Supabase Storage内のパス）
     */
    private String storageKey;
    
    /**
     * 撮影位置（front, right, back, left）
     */
    private String position;
    
    /**
     * 撮影日時
     */
    private OffsetDateTime takenAt;
    
    /**
     * 作成日時
     */
    private OffsetDateTime createdAt;
    
    /**
     * 署名付きURL（画像アクセス用）
     */
    private String signedUrl;
    
    /**
     * 公開同意フラグ
     */
    private boolean consentPublication;
    
    /**
     * PostureImageエンティティからレスポンスDTOに変換
     * 
     * @param entity PostureImageエンティティ
     * @return PostureImageUploadResponse
     */
    public static PostureImageUploadResponse fromEntity(PostureImage entity) {
        if (entity == null) {
            return null;
        }
        
        return PostureImageUploadResponse.builder()
                .id(entity.getId())
                .postureGroupId(entity.getPostureGroup() != null ? entity.getPostureGroup().getId() : null)
                .storageKey(entity.getStorageKey())
                .position(entity.getPosition() != null ? entity.getPosition().getCode() : null)
                .takenAt(entity.getTakenAt())
                .createdAt(entity.getCreatedAt())
                .consentPublication(entity.isConsentPublication())
                .build();
    }
}
