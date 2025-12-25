package com.example.fitnessgym_mg.service;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.UserCustomer;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.repository.UserCustomerRepository;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

/**
 * 顧客へのアクセス権限チェックを一元管理するサービス
 * 認可ロジックをコントローラーから分離し、再利用可能にする
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerAuthorizationService {
    
    private final CustomerRepository customerRepository;
    private final UserCustomerRepository userCustomerRepository;
    private final LessonRepository lessonRepository;
    private final com.example.fitnessgym_mg.repository.PostureImageRepository postureImageRepository;
    private final com.example.fitnessgym_mg.repository.PostureGroupRepository postureGroupRepository;
    private final SecurityUtil securityUtil;
    
    /**
     * 現在のユーザーが指定された顧客にアクセス可能か確認
     * 
     * @param currentUser 現在のユーザー
     * @param customerId 顧客ID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessCustomer(User currentUser, UUID customerId) {
        // ADMINは全顧客にアクセス可能
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }
        
        // 顧客が存在するか確認
        if (!customerRepository.existsById(customerId)) {
            return false;
        }
        
        // MANAGER: 自分の店舗に所属する顧客のみアクセス可能
        if (currentUser.getRole() == UserRole.MANAGER) {
            return canManagerAccessCustomer(currentUser, customerId);
        }
        
        // TRAINER: 担当している顧客のみアクセス可能
        if (currentUser.getRole() == UserRole.TRAINER) {
            return isTrainerAssignedToCustomer(currentUser.getId(), customerId);
        }
        
        // CUSTOMER: 自分のプロフィールのみアクセス可能
        // 実装に応じて調整
        return false;
    }
    
    /**
     * マネージャーが顧客にアクセス可能か確認
     */
    private boolean canManagerAccessCustomer(User manager, UUID customerId) {
        if (manager.getStores() == null || manager.getStores().isEmpty()) {
            return false;
        }
        
        // 顧客の店舗所属を取得（JOIN FETCHで最適化）
        Customer customer = customerRepository.findByIdWithStores(customerId)
            .orElse(null);
        
        if (customer == null || customer.getStores() == null || customer.getStores().isEmpty()) {
            return false;
        }
        
        // ユーザーの店舗と顧客の店舗の交差を確認
        return manager.getStores().stream()
            .anyMatch(userStore -> customer.getStores().stream()
                .anyMatch(customerStore -> customerStore.getId().equals(userStore.getId())));
    }
    
    /**
     * トレーナーが顧客に割り当てられているか確認（存在確認専用クエリ）
     */
    private boolean isTrainerAssignedToCustomer(UUID trainerId, UUID customerId) {
        return userCustomerRepository.existsById(
            new UserCustomer.UserCustomerId(trainerId, customerId)
        );
    }
    
    /**
     * 現在のユーザーが指定された店舗にアクセス可能か確認
     * 
     * @param currentUser 現在のユーザー
     * @param storeId 店舗ID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessStore(User currentUser, UUID storeId) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (currentUser.getStores() == null || currentUser.getStores().isEmpty()) {
            return false;
        }
        return currentUser.getStores().stream()
            .anyMatch(store -> store.getId().equals(storeId));
    }
    
    /**
     * SpEL用: 現在のユーザーが指定された店舗にアクセス可能か確認
     * 
     * <p>@PreAuthorizeのSpELから呼び出すためのメソッド。
     * Authenticationから現在のユーザーを取得してcanAccessStoreを呼び出す。</p>
     * 
     * @param authentication Spring Securityの認証情報
     * @param storeId 店舗ID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessStore(Authentication authentication, UUID storeId) {
        User currentUser = securityUtil.getCurrentUserOrThrow();
        return canAccessStore(currentUser, storeId);
    }
    
    /**
     * SpEL用: 現在のユーザーが指定された顧客にアクセス可能か確認
     * 
     * <p>@PreAuthorizeのSpELから呼び出すためのメソッド。
     * Authenticationから現在のユーザーを取得してcanAccessCustomerを呼び出す。</p>
     * 
     * @param authentication Spring Securityの認証情報
     * @param customerId 顧客ID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessCustomer(Authentication authentication, UUID customerId) {
        User currentUser = securityUtil.getCurrentUserOrThrow();
        return canAccessCustomer(currentUser, customerId);
    }
    
    /**
     * 現在のユーザーが指定されたレッスンにアクセス可能か確認
     * 
     * @param currentUser 現在のユーザー
     * @param lessonId レッスンID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessLesson(User currentUser, UUID lessonId) {
        // ADMINは全レッスンにアクセス可能
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }
        
        // レッスンからcustomerIdを取得
        UUID customerId = lessonRepository.findCustomerIdByLessonId(lessonId)
            .orElse(null);
        
        if (customerId == null) {
            return false;
        }
        
        // 顧客へのアクセス権限を確認
        return canAccessCustomer(currentUser, customerId);
    }
    
    /**
     * 現在のユーザーが指定された顧客にアクセス可能か確認し、不可能な場合は例外をスロー
     * 
     * <p>Service層での認可チェック用メソッド。
     * Controller層での早期リターンとは別に、Service層での最終防衛ラインとして使用する。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param customerId 顧客ID
     * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
     */
    public void checkCanAccessCustomerOrThrow(User currentUser, UUID customerId) {
        if (!canAccessCustomer(currentUser, customerId)) {
            throw new com.example.fitnessgym_mg.exception.AccessDeniedException("この顧客にアクセスする権限がありません");
        }
    }
    
    /**
     * 現在のユーザーが指定されたレッスンにアクセス可能か確認し、不可能な場合は例外をスロー
     * 
     * <p>Service層での認可チェック用メソッド。
     * Controller層での早期リターンとは別に、Service層での最終防衛ラインとして使用する。</p>
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
     * 現在のユーザーが指定された姿勢画像にアクセス可能か確認
     * 
     * @param currentUser 現在のユーザー
     * @param imageId 姿勢画像ID
     * @return アクセス可能な場合 true
     */
    public boolean canAccessPostureImage(User currentUser, UUID imageId) {
        // ADMINは全画像にアクセス可能
        if (currentUser.getRole() == UserRole.ADMIN) {
            return true;
        }
        
        // 画像からpostureGroupIdを取得
        UUID postureGroupId = postureImageRepository.findPostureGroupIdByImageId(imageId)
            .orElse(null);
        
        if (postureGroupId == null) {
            return false;
        }
        
        // PostureGroupからlessonIdを取得
        UUID lessonId = postureGroupRepository.findLessonIdByPostureGroupId(postureGroupId)
            .orElse(null);
        
        if (lessonId == null) {
            return false;
        }
        
        // レッスンへのアクセス権限を確認
        return canAccessLesson(currentUser, lessonId);
    }
    
    /**
     * 現在のユーザーが指定された姿勢画像にアクセス可能か確認し、不可能な場合は例外をスロー
     * 
     * <p>Service層での認可チェック用メソッド。
     * Controller層での早期リターンとは別に、Service層での最終防衛ラインとして使用する。</p>
     * 
     * @param currentUser 現在のユーザー
     * @param imageId 姿勢画像ID
     * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
     */
    public void checkCanAccessPostureImageOrThrow(User currentUser, UUID imageId) {
        if (!canAccessPostureImage(currentUser, imageId)) {
            throw new com.example.fitnessgym_mg.exception.AccessDeniedException("この姿勢画像にアクセスする権限がありません");
        }
    }
}

