package com.example.fitnessgym_mg.service.policy;

import org.springframework.stereotype.Service;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;

/**
 * ロールベースの認可ポリシーを一元管理するService
 * 
 * <p>認可ポリシーを一元管理し、将来的な拡張（SUPER_ADMIN追加、ADMINの一部制限など）にも対応できる設計。</p>
 * 
 * <p>設計方針:</p>
 * <ul>
 *   <li>Spring管理下のServiceとして実装（DIで注入可能）</li>
 *   <li>認可ポリシーの変更はこのクラスのみで対応可能</li>
 *   <li>各AuthorizationServiceからDIで注入して使用</li>
 *   <li>テスト時にモック/スタブに差し替え可能</li>
 * </ul>
 * 
 * <p>現時点のポリシー:</p>
 * <ul>
 *   <li>ADMIN: 全リソースにフルアクセス可能</li>
 * </ul>
 */
@Service
public class RolePolicy {
    
    /**
     * 指定されたユーザーがスーパーユーザー（全リソースにフルアクセス可能）かどうかを判定
     * 
     * <p>現時点ではADMINロールのみをスーパーユーザーとして扱う。
     * 将来的にSUPER_ADMINが追加された場合も、このメソッド内で対応可能。</p>
     * 
     * <p>ポリシー変更例:</p>
     * <ul>
     *   <li>SUPER_ADMIN追加: `currentUser.getRoles().contains(UserRole.SUPER_ADMIN) || currentUser.getRoles().contains(UserRole.ADMIN)`</li>
     *   <li>ADMINの一部制限: 条件を追加して特定のリソースのみ制限</li>
     *   <li>DB参照: Repositoryを注入してDBからポリシーを取得</li>
     *   <li>設定ファイル参照: @Valueで設定値を注入</li>
     *   <li>時限ロール: 現在時刻を参照して有効期限を判定</li>
     * </ul>
     * 
     * @param currentUser 現在のユーザー（nullの場合はfalseを返す）
     * @return スーパーユーザーの場合 true
     */
    public boolean isSuperUser(User currentUser) {
        if (currentUser == null) {
            return false;
        }
        
        // 将来の多ロール対応を考慮し、rolesベースで判定
        // ADMINは全リソースにフルアクセス可能
        return currentUser.getRoles().contains(UserRole.ADMIN);
    }
}

