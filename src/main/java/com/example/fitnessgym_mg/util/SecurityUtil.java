package com.example.fitnessgym_mg.util;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import com.example.fitnessgym_mg.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * セキュリティユーティリティクラス
 * 現在ログイン中のユーザー情報取得や権限チェックを提供
 * 
 * <p><b>設計上の注意:</b></p>
 * <ul>
 *   <li>このクラスはUserRepositoryに依存していますが、現状の設計では必要です。</li>
 *   <li>JWT認証フィルターでprincipalにemail（String）を格納する設計のため、Userエンティティを取得するにはDBアクセスが必要です。</li>
 *   <li>将来的には、principalにuserId(UUID)を格納する設計に変更することで、Repository依存を削減できます。</li>
 *   <li>ただし、その変更はアーキテクチャレベルの変更が必要であり、JwtAuthenticationFilterの修正も必要です。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    /**
     * UserRepositoryへの依存
     * 
     * <p>現状の設計では、principalにemail（String）を格納するため、
     * Userエンティティを取得するにはDBアクセスが必要です。</p>
     * 
     * <p>将来的な改善: principalにuserId(UUID)を格納する設計に変更することで、
     * Repository依存を削減できます。ただし、アーキテクチャレベルの変更が必要です。</p>
     */
    private final UserRepository userRepository;

    /**
     * 現在ログイン中のユーザーを取得
     * 
     * <p><b>パフォーマンス考慮事項:</b>
     * このメソッドは毎回DBアクセスを発生させます。
     * Service層で複数回呼び出す場合は、1回取得して使い回すことを推奨します。</p>
     * 
     * <p>例:</p>
     * <pre>{@code
     * User currentUser = securityUtil.getCurrentUserOrThrow();
     * // 以降、currentUserを再利用
     * }</pre>
     * 
     * <p><b>将来の改善:</b>
     * principalにuserId(UUID)を格納する設計に変更することで、
     * DBアクセスを削減できます。これはアーキテクチャレベルの変更が必要です。</p>
     * 
     * <p>処理の流れ：</p>
     * <ol>
     *   <li>Spring Securityのセキュリティコンテキストから認証情報を取得</li>
     *   <li>認証されていない場合は空のOptionalを返す</li>
     *   <li>認証情報からメールアドレスを取得してユーザーを検索</li>
     * </ol>
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
        
        // 認証済みユーザーの取得なので、active条件を適用
        return userRepository.findByEmailAndActiveTrue(email);
    }

    /**
     * 現在のユーザーが指定されたロールを持っているか確認（enum版）
     * 
     * <p>型安全なAPIを提供します。文字列での権限チェックは推奨されません。</p>
     * 
     * @param role 確認するロール（UserRole enum）
     * @return ロールを持っている場合 true
     */
    public boolean hasRole(UserRole role) {
        return hasAuthority("ROLE_" + role.name());
    }

    /**
     * 現在のユーザーが指定された権限（authority）を持っているか確認
     * 
     * <p>内部実装メソッド。外部からはenum版の{@link #hasRole(UserRole)}を使用してください。</p>
     * 
     * <p>処理の流れ：</p>
     * <ol>
     *   <li>Spring Securityのセキュリティコンテキストから認証情報を取得</li>
     *   <li>認証されていない、または匿名ユーザーの場合は false を返す</li>
     *   <li>認証情報から権限のリストを取得</li>
     *   <li>指定された権限が権限リストに含まれているか確認（完全一致）</li>
     * </ol>
     * 
     * @param authority 確認する権限（例: "ROLE_ADMIN"）
     * @return 権限を持っている場合 true
     */
    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
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
     * 現在ログイン中のユーザーIDを取得（認証されていない場合は例外をスロー）
     * 
     * <p><b>パフォーマンス注意事項:</b>
     * このメソッドは内部で{@link #getCurrentUserOrThrow()}を呼び出すため、
     * DBアクセスが発生します。</p>
     * 
     * <p>同じリクエスト内でユーザーIDとUserエンティティの両方が必要な場合は、
     * 以下のように先にUserエンティティを取得して使い回すことを推奨します:</p>
     * <pre>{@code
     * User currentUser = securityUtil.getCurrentUserOrThrow();
     * UUID userId = currentUser.getId();
     * // 以降、currentUserとuserIdを再利用
     * }</pre>
     * 
     * @return 現在ログイン中のユーザーID
     * @throws AuthenticationException 認証されていない場合
     */
    public UUID getCurrentUserId() {
        return getCurrentUserOrThrow().getId();
    }

    /**
     * AuthenticationからUserエンティティを取得
     * 
     * <p>SpEL用メソッドで使用する。引数のAuthenticationを正として扱う。</p>
     * 
     * @param authentication Spring Securityの認証情報
     * @return Userエンティティ、取得できない場合は空のOptional
     */
    public Optional<User> getUserFromAuthentication(Authentication authentication) {
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
        
        // 認証済みユーザーの取得なので、active条件を適用
        return userRepository.findByEmailAndActiveTrue(email);
    }

    /**
     * AuthenticationからUserエンティティを取得（認証されていない場合は例外をスロー）
     * 
     * <p>SpEL用メソッドで使用する。引数のAuthenticationを正として扱う。</p>
     * 
     * @param authentication Spring Securityの認証情報
     * @return Userエンティティ
     * @throws AuthenticationException 認証されていない場合
     */
    public User getUserFromAuthenticationOrThrow(Authentication authentication) {
        return getUserFromAuthentication(authentication)
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
     * <p><b>防御的プログラミング:</b>
     * String型のprincipalに対して、email形式の検証を実施します。
     * より厳密な検証（正規表現など）も可能ですが、パフォーマンスとのトレードオフを考慮し、
     * 最小限の検証（@を含む、空でない、適切な長さ）を実施しています。</p>
     * 
     * @param principal Authentication の principal
     * @return メールアドレス、取得できない場合は null
     */
    private String extractEmailFromPrincipal(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            // UserDetailsのusernameがemail形式か検証
            if (isValidEmailFormat(username)) {
                return username;
            } else {
                log.warn("UserDetails username does not appear to be an email: {}", username);
                return null;
            }
        } else if (principal instanceof String s) {
            // JWT認証時は principal に email (String) を格納する設計
            if (isValidEmailFormat(s)) {
                return s;
            } else {
                // 予期しないString型のprincipal（email形式でない）
                // Spring Security設定変更、OAuth2/OIDC導入、principalを独自クラスに変更した場合に発生する可能性
                log.warn("Principal is String but does not appear to be an email: {}", s);
                return null;
            }
        }
        return null;
    }

    /**
     * メールアドレス形式の簡易検証
     * 
     * <p>パフォーマンスとのトレードオフを考慮し、最小限の検証を実施します:</p>
     * <ul>
     *   <li>nullまたは空文字でない</li>
     *   <li>@を含む</li>
     *   <li>@の前に少なくとも1文字、@の後に少なくとも1文字（ドメイン部分）</li>
     *   <li>最大長255文字（RFC 5321準拠）</li>
     * </ul>
     * 
     * <p>より厳密な検証が必要な場合は、正規表現やApache Commons Validatorなどを使用できますが、
     * パフォーマンスへの影響を考慮する必要があります。</p>
     * 
     * @param email 検証対象の文字列
     * @return メールアドレス形式として有効な場合 true
     */
    private boolean isValidEmailFormat(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        
        // 長さチェック（RFC 5321準拠: 最大255文字）
        if (email.length() > 255) {
            return false;
        }
        
        // @を含むかチェック
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex >= email.length() - 1) {
            // @が先頭、末尾、または存在しない場合は無効
            return false;
        }
        
        // @の前後に少なくとも1文字ずつあることを確認
        // より厳密な検証（ドットの存在、ドメイン部分の形式など）は必要に応じて追加可能
        
        return true;
    }
}

