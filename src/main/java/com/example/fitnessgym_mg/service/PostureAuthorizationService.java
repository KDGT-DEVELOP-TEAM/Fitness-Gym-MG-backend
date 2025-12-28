package com.example.fitnessgym_mg.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.PostureImageRepository;
import com.example.fitnessgym_mg.service.policy.RolePolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 姿勢画像へのアクセス権限チェックを一元管理するサービス
 * 認可ロジックをコントローラーから分離し、再利用可能にする
 * 
 * <p>設計方針:</p>
 * <ul>
 *   <li>boolean版メソッド（canAccessXxx）: SpEL専用。AuthorizationFacadeに実装。</li>
 *   <li>例外版メソッド（checkCanAccessXxxOrThrow）: Service層での使用を推奨。AuthorizationFacadeに実装。</li>
 * </ul>
 * 
 * <p>このServiceは認可ロジックの内部実装のみを担当し、外部からはAuthorizationFacade経由でアクセスすること。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostureAuthorizationService {
    
    private final PostureImageRepository postureImageRepository;
    private final RolePolicy rolePolicy;
    
    /**
     * 現在のユーザーが指定された姿勢画像にアクセス可能か確認（内部実装）
     * 
     * <p>このメソッドは内部実装用。外部からはAuthorizationFacade経由でアクセスすること。</p>
     * 
     * <p>RepositoryレベルのEXISTSクエリを使用し、「取得」と「可否判定」を混ぜない。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param imageId 姿勢画像ID
     * @return アクセス可能な場合 true
     */
    boolean canAccessPostureImageInternal(User currentUser, UUID imageId) {
        // 不正な引数は「アクセス不可」として扱う（例外は投げない設計）
        if (currentUser == null) {
            log.warn("Authorization check failed: currentUser is null. imageId={}", imageId);
            return false;
        }
        
        if (imageId == null) {
            log.warn("Authorization check failed: imageId is null. userId={}, imageId=null", currentUser.getId());
            return false;
        }
        
        // スーパーユーザー（ADMIN）は全画像にアクセス可能
        if (rolePolicy.isSuperUser(currentUser)) {
            return true;
        }
        
        // RepositoryレベルのEXISTSクエリで、ユーザーが姿勢画像にアクセス可能か確認
        return postureImageRepository.existsAccessiblePostureImage(currentUser.getId(), imageId);
    }
    
    
}

