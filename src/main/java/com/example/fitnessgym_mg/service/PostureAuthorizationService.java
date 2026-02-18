package com.example.fitnessgym_mg.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.repository.CustomerRepositoryCustom;
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
    private final CustomerRepository customerRepository;
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
        
        // MANAGER: 全店舗の姿勢画像にアクセス可能（トレーナーと同様のロジック）
        // 姿勢画像が属する顧客が存在し、論理削除されていない、かつ有効な場合にアクセス可能
        if (currentUser.getRole() == UserRole.MANAGER) {
            boolean result = canManagerAccessPostureImage(currentUser, imageId);
            log.debug("Authorization check: MANAGER access={} for imageId={}", result, imageId);
            return result;
        }
        
        // RepositoryレベルのEXISTSクエリで、ユーザーが姿勢画像にアクセス可能か確認
        return postureImageRepository.existsAccessiblePostureImage(currentUser.getId(), imageId);
    }
    
    /**
     * マネージャーが姿勢画像にアクセス可能か確認
     * 
     * <p>マネージャーは全店舗の姿勢画像にアクセス可能（トレーナーと同様のロジック）</p>
     * <p>姿勢画像が属する顧客が存在し、論理削除されていない、かつ有効な場合にアクセス可能</p>
     * 
     * @param manager マネージャー
     * @param imageId 姿勢画像ID
     * @return アクセス可能な場合 true
     */
    private boolean canManagerAccessPostureImage(User manager, UUID imageId) {
        // マネージャーは全店舗の姿勢画像にアクセス可能（トレーナーと同様のロジック）
        // 姿勢画像が属する顧客が存在し、論理削除されていない、かつ有効な場合にアクセス可能
        try {
            // 姿勢画像から顧客IDを取得
            UUID customerId = postureImageRepository.findCustomerIdByImageId(imageId)
                    .orElse(null);
            
            if (customerId == null) {
                log.debug("canManagerAccessPostureImage: customer not found for image - managerId={}, imageId={}", 
                        manager.getId(), imageId);
                return false;
            }
            
            // 顧客が存在し、論理削除されていない、かつ有効かを確認
            if (customerRepository instanceof CustomerRepositoryCustom) {
                java.util.Optional<Customer> customerOpt = ((CustomerRepositoryCustom) customerRepository)
                        .findByIdWithStoresNative(customerId);
                
                if (customerOpt.isEmpty()) {
                    log.debug("canManagerAccessPostureImage: customer not found - managerId={}, imageId={}, customerId={}", 
                            manager.getId(), imageId, customerId);
                    return false;
                }
                
                Customer customer = customerOpt.get();
                // 顧客が有効で、論理削除されていない場合にアクセス可能
                boolean result = customer.isActive() && !customer.isDeleted();
                log.debug("canManagerAccessPostureImage: managerId={}, imageId={}, customerId={}, active={}, deleted={}, result={}", 
                        manager.getId(), imageId, customerId, customer.isActive(), customer.isDeleted(), result);
                return result;
            } else {
                log.error("CustomerRepository does not implement CustomerRepositoryCustom");
                return false;
            }
        } catch (Exception e) {
            log.error("canManagerAccessPostureImage failed: managerId={}, imageId={}", 
                    manager.getId(), imageId, e);
            return false;
        }
    }
    
}

