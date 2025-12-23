package com.example.fitnessgym_mg.service;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security用のUserDetailsService実装
 * 
 * ログイン処理の流れ：
 * 1. ユーザーがログインフォームにメールアドレスとパスワードを入力
 * 2. Spring SecurityがこのクラスのloadUserByUsername()メソッドを呼び出す
 * 3. データベースからユーザー情報を取得
 * 4. パスワードをBCryptで比較して認証
 * 5. 認証成功後、ユーザー情報をセッションに保存
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * ユーザーリポジトリ
     * データベースからユーザー情報を取得するために使用します。
     * @RequiredArgsConstructorにより、自動的にコンストラクタで注入されます。
     */
    private final UserRepository userRepository;

    /**
     * メールアドレスでユーザー情報を取得し、Spring SecurityのUserDetailsに変換
     * 
     * 処理の流れ：
     * 1. メールアドレスでアクティブなユーザーをデータベースから検索
     * 2. ユーザーが見つからない場合は例外をスロー
     * 3. ユーザー情報をSpring SecurityのUserDetails形式に変換
     * 4. UserDetailsを返す（Spring Securityがパスワード照合などを行う）
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // メールアドレスでアクティブなユーザーを検索
        User user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + email));

        // Spring SecurityのUserDetailsに変換
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword()) // 既にBCryptハッシュ化済み
                .authorities(getAuthorities(user))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isActive())
                .build();
    }

    /**
     * ユーザーのroleフィールドから権限を生成
     * 
     * 処理の流れ：
     * 1. ユーザーのroleフィールドを取得（UserRole型のEnum）
     * 2. Enumのname()メソッドで文字列に変換し、大文字に変換して"ROLE_"プレフィックスを付ける（例："ROLE_ADMIN"）
     * 3. SimpleGrantedAuthorityオブジェクトを作成して返す
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        String role = user.getRole().name(); // UserRole EnumをStringに変換
        String authority = "ROLE_" + role.toUpperCase();
        return Collections.singletonList(new SimpleGrantedAuthority(authority));
    }
}

