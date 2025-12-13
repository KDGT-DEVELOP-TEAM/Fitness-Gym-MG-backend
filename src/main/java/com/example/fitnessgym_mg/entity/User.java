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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "users") // 明示的にテーブル名を指定
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = { "stores" })
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(unique = true, nullable = false)
	private String email;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String kana;

	@Column(nullable = false)
	private String password;

	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false, columnDefinition = "user_role")
	private UserRole role;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public enum UserRole {
		ADMIN, MANAGER, TRAINER;
	}

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

	@Override
	public int hashCode() {
		if (id == null) {
			return super.hashCode();
		}
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		User user = (User) obj;
		return id != null && id.equals(user.id);
	}
}
