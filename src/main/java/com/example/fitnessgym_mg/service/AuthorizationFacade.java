package com.example.fitnessgym_mg.service;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 認可チェックのFacade層
 * 
 * <p>SpEL用メソッドから呼び出されるFacade層。
 * 例外処理を含むビジネスロジックを担当し、SpELメソッドは「変換+委譲」のみを担当する。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationFacade {
    
    private final CustomerAuthorizationService customerAuthorizationService;
    private final StoreAuthorizationService storeAuthorizationService;
    private final LessonAuthorizationService lessonAuthorizationService;
    private final PostureAuthorizationService postureAuthorizationService;
    private final SecurityUtil securityUtil;
    
    /**
     * 顧客へのアクセス権限を確認
     * 
     * <p>判断の最終責任者。すべての認可チェックはこのFacade経由で実施する。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param customerId 顧客ID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessCustomer(User currentUser, UUID customerId) {
        try {
            return customerAuthorizationService.canAccessCustomerInternal(currentUser, customerId);
        } catch (RuntimeException e) {
            log.error("AuthorizationFacade.canAccessCustomer: Authorization check failed unexpectedly - userId={}, customerId={}", 
                    currentUser != null ? currentUser.getId() : "null", customerId, e);
            return false;
        }
    }
    
    /**
     * 有効な顧客（論理削除済みを除く）へのアクセス権限を確認
     * 
     * <p>判断の最終責任者。プロフィール、履歴、姿勢画像など、有効な顧客のみ表示すべきページで使用する。</p>
     * <p>canAccessCustomerとは異なり、ADMINロールでもisActiveとisDeletedのチェックを実施する。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param customerId 顧客ID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessActiveCustomer(User currentUser, UUID customerId) {
        try {
            return customerAuthorizationService.canAccessActiveCustomerInternal(currentUser, customerId);
        } catch (RuntimeException e) {
            log.error("AuthorizationFacade.canAccessActiveCustomer: Authorization check failed unexpectedly - userId={}, customerId={}", 
                    currentUser != null ? currentUser.getId() : "null", customerId, e);
            return false;
        }
    }
    
    /**
     * 店舗へのアクセス権限を確認
     * 
     * <p>判断の最終責任者。すべての認可チェックはこのFacade経由で実施する。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param storeId 店舗ID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessStore(User currentUser, UUID storeId) {
        try {
            return storeAuthorizationService.canAccessStoreInternal(currentUser, storeId);
        } catch (RuntimeException e) {
            log.error("Authorization check failed unexpectedly for storeId={}", storeId, e);
            return false;
        }
    }
    
    /**
     * レッスンへのアクセス権限を確認
     * 
     * <p>判断の最終責任者。すべての認可チェックはこのFacade経由で実施する。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param lessonId レッスンID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessLesson(User currentUser, UUID lessonId) {
        try {
            return lessonAuthorizationService.canAccessLessonInternal(currentUser, lessonId);
        } catch (RuntimeException e) {
            log.error("Authorization check failed unexpectedly for lessonId={}", lessonId, e);
            return false;
        }
    }
    
    /**
     * 姿勢画像へのアクセス権限を確認
     * 
     * <p>判断の最終責任者。すべての認可チェックはこのFacade経由で実施する。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param imageId 画像ID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessPostureImage(User currentUser, UUID imageId) {
        try {
            return postureAuthorizationService.canAccessPostureImageInternal(currentUser, imageId);
        } catch (RuntimeException e) {
            log.error("Authorization check failed unexpectedly for imageId={}", imageId, e);
            return false;
        }
    }
    
    /**
     * 顧客へのアクセス権限を確認し、不可能な場合は例外をスロー
     * 
     * <p>Service層での認可チェック用メソッド。
     * 判断の最終責任者はFacade層。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param customerId 顧客ID
     * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
     */
    public void checkCanAccessCustomerOrThrow(User currentUser, UUID customerId) {
        if (!canAccessCustomer(currentUser, customerId)) {
            String userId = currentUser != null ? currentUser.getId().toString() : "unknown";
            String resource = "customer:" + (customerId != null ? customerId.toString() : "unknown");
            log.warn("Access denied: userId={}, resource={}, role={}", 
                    userId, resource, currentUser != null ? currentUser.getRole() : "unknown");
            throw new com.example.fitnessgym_mg.exception.AccessDeniedException(userId, resource);
        }
    }
    
    /**
     * 有効な顧客（論理削除済みを除く）へのアクセス権限を確認し、不可能な場合は例外をスロー
     * 
     * <p>Service層での認可チェック用メソッド。
     * プロフィール、履歴、姿勢画像など、有効な顧客のみ表示すべきページで使用する。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param customerId 顧客ID
     * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
     */
    public void checkCanAccessActiveCustomerOrThrow(User currentUser, UUID customerId) {
        if (!canAccessActiveCustomer(currentUser, customerId)) {
            String userId = currentUser != null ? currentUser.getId().toString() : "unknown";
            String resource = "activeCustomer:" + (customerId != null ? customerId.toString() : "unknown");
            log.warn("Access denied (active customer): userId={}, resource={}, role={}", 
                    userId, resource, currentUser != null ? currentUser.getRole() : "unknown");
            throw new com.example.fitnessgym_mg.exception.AccessDeniedException(userId, resource);
        }
    }
    
    /**
     * 顧客へのアクセス権限を確認し、不可能な場合は例外をスロー（エンティティ版）
     * 
     * <p>Service層での認可チェック用メソッド。
     * エンティティが既に取得済みの場合に使用する。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param customer 顧客エンティティ
     * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
     */
    public void checkCanAccessCustomerOrThrow(User currentUser, com.example.fitnessgym_mg.entity.Customer customer) {
        if (!canAccessCustomer(currentUser, customer.getId())) {
            throw new com.example.fitnessgym_mg.exception.AccessDeniedException("この顧客にアクセスする権限がありません");
        }
    }
    
    /**
     * 店舗へのアクセス権限を確認し、不可能な場合は例外をスロー
     * 
     * <p>Service層での認可チェック用メソッド。
     * 判断の最終責任者はFacade層。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param storeId 店舗ID
     * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
     */
    public void checkCanAccessStoreOrThrow(User currentUser, UUID storeId) {
        if (!canAccessStore(currentUser, storeId)) {
            throw new com.example.fitnessgym_mg.exception.AccessDeniedException("この店舗にアクセスする権限がありません");
        }
    }
    
    /**
     * レッスンへのアクセス権限を確認し、不可能な場合は例外をスロー
     * 
     * <p>Service層での認可チェック用メソッド。
     * 判断の最終責任者はFacade層。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param lessonId レッスンID
     * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
     */
    public void checkCanAccessLessonOrThrow(User currentUser, UUID lessonId) {
        if (!canAccessLesson(currentUser, lessonId)) {
            throw new com.example.fitnessgym_mg.exception.AccessDeniedException("このレッスンにアクセスする権限がありません");
        }
    }
    
    /**
     * 姿勢画像へのアクセス権限を確認し、不可能な場合は例外をスロー
     * 
     * <p>Service層での認可チェック用メソッド。
     * 判断の最終責任者はFacade層。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param imageId 画像ID
     * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
     */
    public void checkCanAccessPostureImageOrThrow(User currentUser, UUID imageId) {
        if (!canAccessPostureImage(currentUser, imageId)) {
            throw new com.example.fitnessgym_mg.exception.AccessDeniedException("この姿勢画像にアクセスする権限がありません");
        }
    }
    
    /**
     * SpEL用: 現在のユーザーが指定された顧客にアクセス可能か確認
     * 
     * <p>@PreAuthorizeのSpELから呼び出すためのメソッド。
     * AuthenticationからUserを取得し、内部のcanAccessCustomerに委譲する。</p>
     * 
     * @param authentication Spring Securityの認証情報
     * @param customerId 顧客ID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessCustomer(Authentication authentication, UUID customerId) {
        try {
        User currentUser = securityUtil.getUserFromAuthenticationOrThrow(authentication);
        return canAccessCustomer(currentUser, customerId);
        } catch (Exception e) {
            log.error("AuthorizationFacade.canAccessCustomer (SpEL): Failed to extract user or check access - authentication={}, customerId={}", 
                    authentication != null ? authentication.getName() : "null", customerId, e);
            return false;
        }
    }
    
    /**
     * SpEL用: 現在のユーザーが指定された有効な顧客（論理削除済みを除く）にアクセス可能か確認
     * 
     * <p>@PreAuthorizeのSpELから呼び出すためのメソッド。
     * AuthenticationからUserを取得し、内部のcanAccessActiveCustomerに委譲する。</p>
     * <p>プロフィール、履歴、姿勢画像など、有効な顧客のみ表示すべきページで使用する。</p>
     * 
     * @param authentication Spring Securityの認証情報
     * @param customerId 顧客ID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessActiveCustomer(Authentication authentication, UUID customerId) {
        try {
            User currentUser = securityUtil.getUserFromAuthenticationOrThrow(authentication);
            return canAccessActiveCustomer(currentUser, customerId);
        } catch (Exception e) {
            log.error("AuthorizationFacade.canAccessActiveCustomer (SpEL): Failed to extract user or check access - authentication={}, customerId={}", 
                    authentication != null ? authentication.getName() : "null", customerId, e);
            return false;
        }
    }
    
    /**
     * SpEL用: 現在のユーザーが指定された店舗にアクセス可能か確認
     * 
     * <p>@PreAuthorizeのSpELから呼び出すためのメソッド。
     * AuthenticationからUserを取得し、内部のcanAccessStoreに委譲する。</p>
     * 
     * @param authentication Spring Securityの認証情報
     * @param storeId 店舗ID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessStore(Authentication authentication, UUID storeId) {
        User currentUser = securityUtil.getUserFromAuthenticationOrThrow(authentication);
        return canAccessStore(currentUser, storeId);
    }
    
    /**
     * SpEL用: 現在のユーザーが指定されたレッスンにアクセス可能か確認
     * 
     * <p>@PreAuthorizeのSpELから呼び出すためのメソッド。
     * AuthenticationからUserを取得し、内部のcanAccessLessonに委譲する。</p>
     * 
     * @param authentication Spring Securityの認証情報
     * @param lessonId レッスンID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessLesson(Authentication authentication, UUID lessonId) {
        User currentUser = securityUtil.getUserFromAuthenticationOrThrow(authentication);
        return canAccessLesson(currentUser, lessonId);
    }
    
    /**
     * SpEL用: 現在のユーザーが指定された姿勢画像にアクセス可能か確認
     * 
     * <p>@PreAuthorizeのSpELから呼び出すためのメソッド。
     * AuthenticationからUserを取得し、内部のcanAccessPostureImageに委譲する。</p>
     * 
     * @param authentication Spring Securityの認証情報
     * @param imageId 画像ID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessPostureImage(Authentication authentication, UUID imageId) {
        User currentUser = securityUtil.getUserFromAuthenticationOrThrow(authentication);
        return canAccessPostureImage(currentUser, imageId);
    }
    
    /**
     * 顧客検索の認可チェック
     * 
     * <p>storeIdがnullの場合はADMINとMANAGERのみ許可、storeIdが指定されている場合は店舗へのアクセス権を検証する。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param storeId 店舗ID（nullの場合は全店舗検索）
     * @throws AccessDeniedException 認可不可の場合
     */
    public void checkCanSearchCustomers(User currentUser, UUID storeId) {
        // storeIdがnullの場合はADMINとMANAGERのみ許可
        if (storeId == null) {
            if (currentUser.getRole() != com.example.fitnessgym_mg.entity.enums.UserRole.ADMIN && 
                currentUser.getRole() != com.example.fitnessgym_mg.entity.enums.UserRole.MANAGER) {
                throw new com.example.fitnessgym_mg.exception.AccessDeniedException("全店舗の顧客を取得できるのはADMINとMANAGERのみです");
            }
        } else {
            // storeIdが指定されている場合、その店舗へのアクセス権を検証
            checkCanAccessStoreOrThrow(currentUser, storeId);
        }
    }
}

