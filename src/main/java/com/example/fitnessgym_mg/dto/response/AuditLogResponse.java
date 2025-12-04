package com.example.fitnessgym_mg.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 監査ログレスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    
    private UUID id;
    private UUID userId;
    private String userName;
    private String action;
    private String targetTable;
    private UUID targetId;
    private LocalDateTime createdAt;
}

