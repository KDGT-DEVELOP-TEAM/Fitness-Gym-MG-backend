package com.example.fitnessgym_mg.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureImageUploadResponse {
    private UUID id;
    private UUID postureGroupId;
    private String storageKey;
    private String position;
    private OffsetDateTime takenAt;
    private OffsetDateTime createdAt;
    private String signedUrl;
    private boolean consentPublication;
}
