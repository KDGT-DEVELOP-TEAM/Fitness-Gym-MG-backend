package com.example.fitnessgym_mg.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.UUID;

@Data
public class BatchSignedUrlRequest {
    @NotNull(message = "imageIdsは必須です")
    private List<UUID> imageIds;
    
    @Min(value = 60, message = "expiresInは60秒以上である必要があります")
    @Max(value = 604800, message = "expiresInは604800秒（7日）以下である必要があります")
    private int expiresIn = 3600; // デフォルト: 1時間
}
