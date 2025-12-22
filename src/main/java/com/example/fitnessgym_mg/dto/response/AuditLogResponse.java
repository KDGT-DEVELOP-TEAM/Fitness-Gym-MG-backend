package com.example.fitnessgym_mg.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.AuditLog;

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
    
    /**
     * AuditLogエンティティからレスポンスDTOに変換
     * 
     * @param entity AuditLogエンティティ
     * @return AuditLogResponse
     */
    public static AuditLogResponse fromEntity(AuditLog entity) {
        if (entity == null) {
            return null;
        }
        
        // targetIdはString型だが、UUIDに変換を試みる（変換できない場合はnull）
        UUID targetIdUuid = null;
        try {
            if (entity.getTargetId() != null && !entity.getTargetId().isEmpty()) {
                targetIdUuid = UUID.fromString(entity.getTargetId());
            }
        } catch (IllegalArgumentException e) {
            // UUID形式でない場合はnullのまま
        }
        
        return AuditLogResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .userName(entity.getUser() != null ? entity.getUser().getName() : null)
                .action(entity.getAction())
                .targetTable(entity.getTargetTable())
                .targetId(targetIdUuid)
                .createdAt(entity.getCreatedAt() != null 
                        ? entity.getCreatedAt().toLocalDateTime() 
                        : null)
                .build();
    }
}

