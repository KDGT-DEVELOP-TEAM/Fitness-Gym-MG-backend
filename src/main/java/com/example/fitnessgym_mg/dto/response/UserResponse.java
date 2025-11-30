package com.example.fitnessgym_mg.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.User;

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
public class UserResponse {
    private UUID id;
    private String email;
    private String kana;
    private String name;
    
    // roleはEnumのcode値（"admin"/"manager"/"trainer"）を使用
    private String role;
    
    private boolean active;
    private OffsetDateTime createdAt;
    
    /**
     * UserエンティティからレスポンスDTOに変換
     */
    public static UserResponse fromEntity(User entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .kana(entity.getKana())
                .name(entity.getName())
                .role(entity.getRole().name().toLowerCase()) // Enum → code変換
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

