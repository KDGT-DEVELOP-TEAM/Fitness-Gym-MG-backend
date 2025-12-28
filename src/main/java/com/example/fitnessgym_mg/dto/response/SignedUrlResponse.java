package com.example.fitnessgym_mg.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

/**
 * 署名付きURLレスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignedUrlResponse {
    private String signedUrl;
    private OffsetDateTime expiresAt;
}
