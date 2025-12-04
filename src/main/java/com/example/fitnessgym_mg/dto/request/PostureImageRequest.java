package com.example.fitnessgym_mg.dto.request;

import java.time.OffsetDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostureImageRequest {
    
    @NotBlank(message = "ストレージキーは必須です")
    private String storageKey;
    
    @NotNull(message = "撮影方向は必須です")
    private String position; // "front", "right", "back", "left"
    
    private boolean consentPublication = false;
    
    private OffsetDateTime takenAt; // 撮影日時（省略時は現在時刻）
}

