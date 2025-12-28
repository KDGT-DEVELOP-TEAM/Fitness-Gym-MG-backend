package com.example.fitnessgym_mg.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.example.fitnessgym_mg.entity.enums.UserRole;

/**
 * ログインレスポンスDTO
 * 認証成功時に返されるユーザー情報とJWTトークンを含む
 * 
 * <p><strong>責務</strong>: 認証結果とユーザー初期表示に必要な基本情報を返します。</p>
 * <p>含まれる情報:</p>
 * <ul>
 *   <li>ユーザーID、メールアドレス、ユーザー名: 初期表示用</li>
 *   <li>ユーザーロール: 認可判定用</li>
 *   <li>JWTトークン: 認証情報</li>
 * </ul>
 * <p>このDTOは認証結果とユーザー基本情報を返します。</p>
 * <p>トークンの保存方法や使用方法については、API仕様書を参照してください。</p>
 * 
 * <p>セキュリティ: JWTトークンはログ出力から除外します。</p>
 */
@Data
@ToString(exclude = "token") // セキュリティ: JWTトークンをログに出力しない
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
     * ユーザーロール
     * 
     * <p>ADMIN、MANAGER、TRAINERのいずれかの値を持ちます。</p>
     * <p>JSONシリアライズ時は文字列として出力されます。</p>
     */
    private UserRole role;
    
    /**
     * JWTトークン
     * 
     * <p>クライアント側で認証情報として保持されます。</p>
     * <p>以降のリクエストでAuthorization: Bearer &lt;token&gt; ヘッダーを付与してください。</p>
     */
    private String token;
}

