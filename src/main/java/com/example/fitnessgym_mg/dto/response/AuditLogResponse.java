package com.example.fitnessgym_mg.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.AuditLog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 監査ログレスポンスDTO
 * 
 * <p>セキュリティ: 個人情報（userName、customerName）はログ出力から除外します。</p>
 */
@Data
@ToString(exclude = {"userName", "customerName"}) // セキュリティ: PIIをログに出力しない
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
    private UUID customerId;
    private String customerName;
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
        
        return AuditLogResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .userName(entity.getUser() != null ? entity.getUser().getName() : null)
                .action(entity.getAction() != null ? entity.getAction().name() : null)
                .targetTable(entity.getTargetTable() != null ? entity.getTargetTable().getTableName() : null)
                .targetId(entity.getTargetId())
                .createdAt(entity.getCreatedAt() != null 
                        ? entity.getCreatedAt().toLocalDateTime() 
                        : null)
                .build();
    }
}

