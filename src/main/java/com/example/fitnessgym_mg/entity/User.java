package com.example.fitnessgym_mg.entity;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.DynamicUpdate;

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
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/**
	 * メールアドレス（ユニーク制約あり）
	 */
	@Column(unique = true, nullable = false, length = 255)
	private String email;

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
	 * ユーザーロール（ADMIN, MANAGER, TRAINER）
	 */
	@jakarta.persistence.Convert(converter = com.example.fitnessgym_mg.entity.converter.UserRoleConverter.class)
	@Column(nullable = false, columnDefinition = "user_role")
	private com.example.fitnessgym_mg.entity.enums.UserRole role;

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
		this.createdAt = LocalDateTime.now();
	}
}
