package com.example.fitnessgym_mg.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcType;

import com.example.fitnessgym_mg.entity.enums.PasswordResetStatus;
import com.example.fitnessgym_mg.entity.type.PasswordResetStatusType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * パスワードリセットリクエストエンティティ
 * ユーザーからのパスワードリセットリクエストを管理
 */
@Entity
@Table(name = "password_reset_requests")
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "user", "processedBy" })
public class PasswordResetRequest {

	@EqualsAndHashCode.Include
	@Id
	@Default
	private UUID id = UUID.randomUUID();

	/**
	 * リクエスト者のメールアドレス
	 */
	@Column(nullable = false, length = 255)
	private String email;

	/**
	 * リクエスト者の申告名
	 */
	@Column(nullable = false, length = 100)
	private String name;

	/**
	 * 照合されたユーザーID（承認時に設定）
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	/**
	 * リクエスト状態
	 * <p>PostgreSQL ENUM型（password_reset_status）としてマッピング</p>
	 * <p>カスタム型（PasswordResetStatusType）を使用してcode値（"pending", "approved", "rejected"）でマッピングします。</p>
	 */
	@JdbcType(PasswordResetStatusType.class)
	@Column(nullable = false, columnDefinition = "password_reset_status")
	@Default
	private PasswordResetStatus status = PasswordResetStatus.PENDING;

	/**
	 * リクエスト日時
	 */
	@Column(name = "requested_at", nullable = false, updatable = false)
	private OffsetDateTime requestedAt;

	/**
	 * 処理日時
	 */
	@Column(name = "processed_at")
	private OffsetDateTime processedAt;

	/**
	 * 処理した管理者ID
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "processed_by_user_id")
	private User processedBy;

	/**
	 * 管理者メモ（任意）
	 */
	@Column(length = 1000)
	private String note;

	@PrePersist
	public void onPrePersist() {
		if (this.requestedAt == null) {
			this.requestedAt = OffsetDateTime.now(ZoneOffset.UTC);
		}
		if (this.status == null) {
			this.status = PasswordResetStatus.PENDING;
		}
	}
}
