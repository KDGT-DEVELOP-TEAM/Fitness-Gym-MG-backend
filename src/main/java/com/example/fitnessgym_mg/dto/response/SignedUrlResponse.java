package com.example.fitnessgym_mg.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class SignedUrlResponse {
    private String signedUrl;
    private OffsetDateTime expiresAt;
}
