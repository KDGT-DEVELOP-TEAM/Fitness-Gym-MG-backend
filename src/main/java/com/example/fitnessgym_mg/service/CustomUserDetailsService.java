package com.example.fitnessgym_mg.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
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
        // メールアドレスでアクティブなユーザーを検索（Repository側でactive条件を適用）
        User user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found or inactive. emailHash={}", hashEmail(email));
                    return new UsernameNotFoundException("ユーザー名またはパスワードが正しくありません");
                });

        // Spring SecurityのUserDetailsに変換
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword()) // 既にBCryptハッシュ化済み
                .authorities(getAuthorities(user))
                .accountExpired(user.isAccountExpired())
                .accountLocked(user.isLocked())
                .credentialsExpired(user.isPasswordExpired())
                .disabled(!user.isActive())
                .build();
    }

    /**
     * ユーザーのロールから権限を生成
     * 
     * <p>処理の流れ：</p>
     * <ol>
     *   <li>ユーザーのgetRoles()メソッドでロールリストを取得（現時点では単一ロール）</li>
     *   <li>各ロールをEnumのname()メソッドで文字列に変換し、"ROLE_"プレフィックスを付ける（例："ROLE_ADMIN"）</li>
     *   <li>SimpleGrantedAuthorityオブジェクトのリストを作成して返す</li>
     * </ol>
     * 
     * <p>将来の多ロール対応を見据えて、Listを返す構造になっている。</p>
     * 
     * <p>防御的プログラミング: nullまたはemptyの場合は空のリストを返す。</p>
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of();
        }
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }

    /**
     * メールアドレスをSHA-256ハッシュ化（ログ出力用）
     * 
     * <p>個人情報保護のため、ログにはメールアドレスを直接出力せず、ハッシュ値を出力する。</p>
     * 
     * @param email メールアドレス
     * @return SHA-256ハッシュ値（16進数文字列）
     */
    private String hashEmail(String email) {
        if (email == null) {
            return "null";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(email.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256は標準アルゴリズムなので、この例外は発生しないはず
            log.error("SHA-256 algorithm not found", e);
            return "hash_error";
        }
    }
}

