package com.example.fitnessgym_mg.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import com.example.fitnessgym_mg.service.policy.RolePolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 店舗へのアクセス権限チェックを一元管理するサービス
 * 認可ロジックをコントローラーから分離し、再利用可能にする
 * 
 * <p>設計方針:</p>
 * <ul>
 *   <li>boolean版メソッド（canAccessXxx）: SpEL専用。AuthorizationFacadeに実装。</li>
 * </ul>
 * 
 * <p>このServiceは認可ロジックの内部実装のみを担当し、外部からはAuthorizationFacade経由でアクセスすること。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreAuthorizationService {
    
    private final RolePolicy rolePolicy;
    
    /**
     * 現在のユーザーが指定された店舗にアクセス可能か確認（内部実装）
     * 
     * <p>このメソッドは内部実装用。外部からはAuthorizationFacade経由でアクセスすること。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param storeId 店舗ID
     * @return アクセス可能な場合 true
     */
    boolean canAccessStoreInternal(User currentUser, UUID storeId) {
        // 不正な引数は「アクセス不可」として扱う（例外は投げない設計）
        if (currentUser == null) {
            log.warn("Authorization check failed: currentUser is null. storeId={}", storeId);
            return false;
        }
        
        if (storeId == null) {
            log.warn("Authorization check failed: storeId is null. userId={}, storeId=null", currentUser.getId());
            return false;
        }
        
        // ADMINとMANAGERは全店舗にアクセス可能
        if (rolePolicy.isSuperUser(currentUser) || 
            currentUser.getRole() == UserRole.MANAGER) {
            return true;
        }
        
        // その他のロールは所属店舗のみ
        if (currentUser.getStores() == null || currentUser.getStores().isEmpty()) {
            return false;
        }
        return currentUser.getStores().stream()
            .anyMatch(store -> store.getId().equals(storeId));
    }
    
}

