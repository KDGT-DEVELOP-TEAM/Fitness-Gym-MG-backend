package com.example.fitnessgym_mg.util;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * セキュリティユーティリティクラス
 * 現在ログイン中のユーザー情報取得や権限チェックを提供
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    /**
     * 現在ログイン中のユーザーを取得
     * 
     * 処理の流れ：
     * 1. Spring Securityのセキュリティコンテキストから認証情報を取得
     * 2. 認証されていない場合は空のOptionalを返す
     * 3. 認証情報からメールアドレスを取得してユーザーを検索
     */
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }
        
        String email = authentication.getName();
        return userRepository.findByEmail(email);
    }

    /**
     * 現在のユーザーが指定されたロールを持っているか確認
     * 
     * 処理の流れ：
     * 1. Spring Securityのセキュリティコンテキストから認証情報を取得
     * 2. 認証情報から権限のリストを取得
     * 3. 指定されたロールが権限リストに含まれているか確認
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(role));
    }

    /**
     * 現在のユーザーがADMINロールを持っているか確認
     */
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * 現在のユーザーがMANAGERロールを持っているか確認
     */
    public boolean isManager() {
        return hasRole("ROLE_MANAGER");
    }

    /**
     * 現在のユーザーがTRAINERロールを持っているか確認
     */
    public boolean isTrainer() {
        return hasRole("ROLE_TRAINER");
    }

    /**
     * 現在ログイン中のユーザーのメールアドレスを取得
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        return authentication.getName();
    }
}

