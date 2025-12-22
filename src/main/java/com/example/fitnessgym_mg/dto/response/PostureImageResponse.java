package com.example.fitnessgym_mg.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.PostureImage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 姿勢画像レスポンスDTO
 * 姿勢画像情報をAPIレスポンスとして返す際に使用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureImageResponse {

    /**
     * 画像ID
     */
    private UUID id;
    
    /**
     * ストレージキー（Supabase Storage内のパス）
     */
    private String storageKey;
    
    /**
     * 公開同意フラグ
     */
    private boolean consentPublication;
    
    /**
     * 撮影日時
     */
    private OffsetDateTime takenAt;
    
    /**
     * 撮影位置（Enumのcode値: "front", "back", "left", "right"）
     */
    private String position;
    
    /**
     * PostureImageエンティティからレスポンスDTOに変換
     */
    public static PostureImageResponse fromEntity(PostureImage entity) {
        if (entity == null) {
            return null;
        }
        
        return PostureImageResponse.builder()
                .id(entity.getId())
                .storageKey(entity.getStorageKey())
                .consentPublication(entity.isConsentPublication())
                .takenAt(entity.getTakenAt())
                .position(entity.getPosition() != null ? entity.getPosition().getCode() : null) // Enum → code変換
                .build();
    }
}

