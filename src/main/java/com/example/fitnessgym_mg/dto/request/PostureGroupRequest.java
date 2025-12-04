package com.example.fitnessgym_mg.dto.request;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostureGroupRequest {
    
    @NotNull(message = "レッスンIDは必須です")
    private UUID lessonId;
    
    private OffsetDateTime capturedAt; // 撮影日時（省略時は現在時刻）
}

