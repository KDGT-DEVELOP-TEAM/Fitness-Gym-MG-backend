package com.example.fitnessgym_mg.entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcType;

import com.example.fitnessgym_mg.entity.type.UserRoleType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * ユーザーエンティティ
 * システムユーザー（管理者、店長、トレーナー）を表す
 */
@Entity
@Table(name = "users") // 明示的にテーブル名を指定
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "stores", "password" })
public class User {

	@EqualsAndHashCode.Include
	@Id
	private UUID id = UUID.randomUUID();

	/**
	 * メールアドレス（ユニーク制約あり）
	 */
	@Column(unique = true, nullable = false, length = 255)
	private String email;

	/**
	 * メールアドレスを設定
	 * 
	 * <p>注意: バリデーションはDTO層（UserRequest）で実施します。
	 * エンティティ層では基本的なnullチェックのみを行います。</p>
	 * 
	 * @param email メールアドレス
	 * @throws IllegalArgumentException メールアドレスが255文字を超える場合
	 */
	public void setEmail(String email) {
		// JPAの永続化時に例外が発生しないよう、バリデーションは最小限に
		// 詳細なバリデーションはDTO層で実施済み
		if (email != null && email.length() > 255) {
			throw new IllegalArgumentException("メールアドレスは255文字以内で入力してください");
		}
		this.email = email;
	}

	/**
	 * ユーザー名
	 */
	@Column(nullable = false, length = 100)
	private String name;

	/**
	 * フリガナ
	 */
	@Column(nullable = false, length = 100)
	private String kana;

	/**
	 * パスワード（ハッシュ化済み）
	 */
	@Column(name = "pass", nullable = false, length = 255)
	private String password;

	/**
	 * パスワードハッシュを設定
	 * 
	 * <p>注意: パスワードのバリデーションはDTO層（UserRequest）で実施します。
	 * このメソッドは既にハッシュ化されたパスワードを受け取ることを前提とします。</p>
	 * 
	 * @param passwordHash BCryptハッシュ化済みパスワード
	 * @throws IllegalArgumentException パスワードハッシュが255文字を超える場合
	 */
	public void setPassword(String passwordHash) {
		// ハッシュ化済みパスワードの長さチェックのみ
		if (passwordHash != null && passwordHash.length() > 255) {
			throw new IllegalArgumentException("パスワードハッシュは255文字以内で入力してください");
		}
		this.password = passwordHash;
	}

	/**
	 * ユーザーロール（ADMIN, MANAGER, TRAINER）
	 * <p>PostgreSQL ENUM型（user_role）としてマッピング</p>
	 * <p>カスタム型（UserRoleType）を使用してcode値（"admin", "manager", "trainer"）でマッピングします。</p>
	 */
	@JdbcType(UserRoleType.class)
	@Column(nullable = false, columnDefinition = "user_role")
	private com.example.fitnessgym_mg.entity.enums.UserRole role;

	/**
	 * Supabase AuthユーザーID
	 * <p>Supabaseのauth.usersテーブルとの紐付けに使用します。</p>
	 */
	@Column(name = "auth_user_id")
	private UUID authUserId;

	/**
	 * 有効/無効フラグ
	 */
	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	/**
	 * 作成日時
	 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@ManyToMany
	@JoinTable(name = "user_stores", // 中間テーブル名
			joinColumns = @JoinColumn(name = "user_id", nullable = false), // User側のFK
			inverseJoinColumns = @JoinColumn(name = "store_id", nullable = false), // Store側のFK
			uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "store_id" })) //複合ユニーク制約
	private Set<Store> stores; // ユーザーが所属する店舗リスト

	@PrePersist
	public void onPrePersist() {
		this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
	}

	/**
	 * アカウントが期限切れかどうかを判定
	 * 
	 * <p>現時点では常にfalseを返す。将来の拡張に対応するため、メソッドとして定義。</p>
	 * 
	 * @return アカウントが期限切れの場合 true
	 */
	public boolean isAccountExpired() {
		// 将来的には accountExpiredAt フィールドと比較する実装を追加
		return false;
	}

	/**
	 * パスワードが期限切れかどうかを判定
	 * 
	 * <p>現時点では常にfalseを返す。将来の拡張に対応するため、メソッドとして定義。</p>
	 * 
	 * @return パスワードが期限切れの場合 true
	 */
	public boolean isPasswordExpired() {
		// 将来的には passwordExpiredAt フィールドと比較する実装を追加
		return false;
	}

	/**
	 * アカウントがロックされているかどうかを判定
	 * 
	 * <p>現時点では常にfalseを返す。将来の拡張に対応するため、メソッドとして定義。</p>
	 * 
	 * @return アカウントがロックされている場合 true
	 */
	public boolean isLocked() {
		// 将来的には lockedAt フィールドと比較する実装を追加
		return false;
	}

	/**
	 * ユーザーのロールリストを取得
	 * 
	 * <p>現時点では単一ロールを返す。将来の多ロール対応を見据えて、Listを返すメソッドとして定義。</p>
	 * 
	 * @return ロールのリスト
	 */
	public List<com.example.fitnessgym_mg.entity.enums.UserRole> getRoles() {
		// 現時点では単一ロールを返す
		return List.of(this.role);
		// 将来的には roles フィールド（Set<UserRole>）から取得する実装に変更
	}
}
