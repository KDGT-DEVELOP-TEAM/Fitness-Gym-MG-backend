package com.example.fitnessgym_mg.util;

import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;
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
        
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        
        // principal から email を取得
        String email = extractEmailFromPrincipal(authentication.getPrincipal());
        if (email == null) {
            return Optional.empty();
        }
        
        return userRepository.findByEmail(email);
    }

    /**
     * 現在のユーザーが指定されたロールを持っているか確認
     * 
     * 処理の流れ：
     * 1. Spring Securityのセキュリティコンテキストから認証情報を取得
     * 2. 認証されていない、または匿名ユーザーの場合は false を返す
     * 3. 認証情報から権限のリストを取得
     * 4. 指定されたロールが権限リストに含まれているか確認
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(role));
    }

    /**
     * 現在のユーザーが指定されたロールを持っているか確認（enum版）
     * 
     * @param role 確認するロール（UserRole enum）
     * @return ロールを持っている場合 true
     */
    public boolean hasRole(UserRole role) {
        return hasRole("ROLE_" + role.name());
    }

    /**
     * 現在のユーザーがADMINロールを持っているか確認
     */
    public boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }

    /**
     * 現在のユーザーがMANAGERロールを持っているか確認
     */
    public boolean isManager() {
        return hasRole(UserRole.MANAGER);
    }

    /**
     * 現在のユーザーがTRAINERロールを持っているか確認
     */
    public boolean isTrainer() {
        return hasRole(UserRole.TRAINER);
    }

    /**
     * 現在ログイン中のユーザーのメールアドレスを取得
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        
        return extractEmailFromPrincipal(authentication.getPrincipal());
    }

    /**
     * 現在ログイン中のユーザーを取得（認証されていない場合は例外をスロー）
     * 
     * @return 現在ログイン中のユーザー
     * @throws AuthenticationException 認証されていない場合
     */
    public User getCurrentUserOrThrow() {
        return getCurrentUser()
                .orElseThrow(() -> new com.example.fitnessgym_mg.exception.AuthenticationException("ログインユーザーが見つかりません"));
    }

    /**
     * Authentication の principal からメールアドレスを取得
     * 
     * <p>設計仕様:</p>
     * <ul>
     *   <li>JWT認証の場合: principal は String (email) を格納する設計</li>
     *   <li>フォーム認証の場合: principal は UserDetails (username = email)</li>
     * </ul>
     * 
     * <p>JWT認証時の principal は {@link com.example.fitnessgym_mg.config.security.JwtAuthenticationFilter}
     * で email として設定されるため、String の場合は email として扱う。</p>
     * 
     * @param principal Authentication の principal
     * @return メールアドレス、取得できない場合は null
     */
    private String extractEmailFromPrincipal(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof String) {
            // JWT認証時は principal に email (String) を格納する設計
            return (String) principal;
        }
        return null;
    }
}

