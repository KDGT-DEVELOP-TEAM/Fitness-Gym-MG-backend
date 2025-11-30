package com.example.fitnessgym_mg.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.PostureImage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureImageResponse {

    private UUID id;
    private String storageKey;
    private boolean consentPublication;
    private OffsetDateTime takenAt;
    
    // positionはEnumのcode値（"front"/"back"/"left"/"right"）を使用
    private String position;
    
    /**
     * PostureImageエンティティからレスポンスDTOに変換
     */
    public static PostureImageResponse fromEntity(PostureImage entity) {
        return PostureImageResponse.builder()
                .id(entity.getId())
                .storageKey(entity.getStorageKey())
                .consentPublication(entity.isConsentPublication())
                .takenAt(entity.getTakenAt())
                .position(entity.getPosition().getCode()) // Enum → code変換
                .build();
    }
}

