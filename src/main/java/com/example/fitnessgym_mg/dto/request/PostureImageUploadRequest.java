package com.example.fitnessgym_mg.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class PostureImageUploadRequest {
    @NotNull(message = "postureGroupIdは必須です")
    private UUID postureGroupId;
    
    @NotNull(message = "positionは必須です")
    @Pattern(regexp = "front|right|back|left", message = "positionはfront, right, back, leftのいずれかである必要があります")
    private String position;
    
    private boolean consentPublication = false;
    
    private OffsetDateTime takenAt; // nullの場合は現在時刻を使用
}
