package com.example.fitnessgym_mg.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ログインレスポンスDTO
 * 認証成功時に返されるユーザー情報とJWTトークンを含む
 * 
 * <p>注意: フロントエンドは取得したトークンをlocalStorageに保存し、
 * 以降のリクエストでAuthorization: Bearer &lt;token&gt; ヘッダーを付与してください。</p>
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
     * JWTトークン
     * フロントエンドはこのトークンをlocalStorageに保存する
     */
    private String token;
}

