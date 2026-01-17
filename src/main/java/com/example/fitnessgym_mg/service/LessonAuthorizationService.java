package com.example.fitnessgym_mg.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.repository.CustomerRepositoryCustom;
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.service.policy.RolePolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * レッスンへのアクセス権限チェックを一元管理するサービス
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
public class LessonAuthorizationService {
    
    private final LessonRepository lessonRepository;
    private final RolePolicy rolePolicy;
    private final CustomerAuthorizationService customerAuthorizationService;
    private final CustomerRepository customerRepository;
    
    /**
     * 現在のユーザーが指定されたレッスンにアクセス可能か確認（内部実装）
     * 
     * <p>このメソッドは内部実装用。外部からはAuthorizationFacade経由でアクセスすること。</p>
     * 
     * <p>レッスン履歴一覧と同じ認可ロジックを使用するため、レッスンIDから顧客IDを取得し、
     * 顧客へのアクセス権限をチェックする。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param lessonId レッスンID
     * @return アクセス可能な場合 true
     */
    boolean canAccessLessonInternal(User currentUser, UUID lessonId) {
        // 不正な引数は「アクセス不可」として扱う（例外は投げない設計）
        if (currentUser == null) {
            log.warn("Authorization check failed: currentUser is null. lessonId={}", lessonId);
            return false;
        }
        
        if (lessonId == null) {
            log.warn("Authorization check failed: lessonId is null. userId={}, lessonId=null", currentUser.getId());
            return false;
        }
        
        // レッスンIDから顧客IDを取得
        UUID customerId = lessonRepository.findCustomerIdByLessonId(lessonId)
            .orElse(null);
        
        if (customerId == null) {
            log.warn("Authorization check failed: Lesson not found. lessonId={}", lessonId);
            return false;
        }
        
        // すべてのロール（ADMIN、MANAGER、TRAINER）で削除された顧客のレッスンも閲覧可能
        // 顧客が存在するかどうかのみを確認し、削除状態はチェックしない
        return canAccessLessonForDeletedCustomer(currentUser, customerId);
    }
    
    /**
     * 削除された顧客のレッスンにアクセス可能か確認（すべてのロール共通）
     * 
     * <p>顧客が存在するかどうかのみを確認し、削除状態はチェックしない。</p>
     * <p>すべてのロール（ADMIN、MANAGER、TRAINER）で削除された顧客のレッスンも閲覧可能とする。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param customerId 顧客ID
     * @return アクセス可能な場合 true
     */
    private boolean canAccessLessonForDeletedCustomer(User currentUser, UUID customerId) {
        // 顧客が存在するかどうかのみを確認（削除状態はチェックしない）
        try {
            if (customerRepository instanceof CustomerRepositoryCustom) {
                java.util.Optional<Customer> customerOpt = ((CustomerRepositoryCustom) customerRepository)
                        .findByIdWithStoresNativeIncludingDeleted(customerId);
                
                if (customerOpt.isEmpty()) {
                    log.debug("canAccessLessonForDeletedCustomer: customer not found - userId={}, role={}, customerId={}", 
                            currentUser.getId(), currentUser.getRole(), customerId);
                    return false;
                }
                
                // 顧客が存在する場合はアクセス可能（削除状態はチェックしない）
                log.debug("canAccessLessonForDeletedCustomer: userId={}, role={}, customerId={}, access granted", 
                        currentUser.getId(), currentUser.getRole(), customerId);
                return true;
            } else {
                log.error("CustomerRepository does not implement CustomerRepositoryCustom");
                return false;
            }
        } catch (Exception e) {
            log.error("canAccessLessonForDeletedCustomer failed: userId={}, role={}, customerId={}", 
                    currentUser.getId(), currentUser.getRole(), customerId, e);
            return false;
        }
    }
    
    
}

