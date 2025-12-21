package com.example.fitnessgym_mg.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class BatchSignedUrlResponse {
    private List<ImageSignedUrl> urls;
    
    @Data
    @AllArgsConstructor
    public static class ImageSignedUrl {
        private UUID imageId;
        private String signedUrl;
        private OffsetDateTime expiresAt;
    }
}
