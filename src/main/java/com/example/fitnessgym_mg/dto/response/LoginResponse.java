package com.example.fitnessgym_mg.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ログインレスポンスDTO
 * 認証成功時に返されるユーザー情報とトークンを含む
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    /**
     * ユーザーID
     */
    private UUID userId;
    
    /**
     * メールアドレス
     */
    private String email;
    
    /**
     * ユーザー名
     */
    private String name;
    
    /**
     * ユーザーロール（ADMIN, MANAGER, TRAINERなど）
     */
    private String role;
    
    /**
     * セッションIDまたはJWTトークン（将来の拡張用）
     */
    private String token;
}

