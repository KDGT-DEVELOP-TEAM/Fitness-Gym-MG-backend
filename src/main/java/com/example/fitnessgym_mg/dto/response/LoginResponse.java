package com.example.fitnessgym_mg.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    private UUID userId;
    private String email;
    private String name;
    private String role;
    private String token; // セッションIDまたはJWTトークン（将来の拡張用）
}

