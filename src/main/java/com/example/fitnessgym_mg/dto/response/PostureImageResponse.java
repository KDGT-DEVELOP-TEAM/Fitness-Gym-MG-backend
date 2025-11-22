package com.example.fitnessgym_mg.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

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
    private String position;
}

