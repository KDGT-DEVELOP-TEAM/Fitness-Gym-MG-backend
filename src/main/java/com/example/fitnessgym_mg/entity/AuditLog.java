package com.example.fitnessgym_mg.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicUpdate;

import com.example.fitnessgym_mg.entity.enums.ActionType;
import com.example.fitnessgym_mg.entity.enums.TargetTableType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 監査ログエンティティ
 * システム操作の記録を保持
 */
@Entity
@Table(name = "logs")
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "user" })
public class AuditLog {

	@EqualsAndHashCode.Include
	@Id
	@Default
	private UUID id = UUID.randomUUID();

	/**
	 * 操作を実行したユーザー（必須リレーション）
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/**
	 * 操作種別（CREATE, UPDATE, DELETE）
	 */
	@Column(nullable = false, length = 50)
	@Enumerated(EnumType.STRING)
	private ActionType action;

	/**
	 * 対象テーブル名
	 */
	@Column(name = "target_table", nullable = false, length = 50)
	@Enumerated(EnumType.STRING)
	private TargetTableType targetTable;

	/**
	 * 対象レコードID（UUID形式）
	 */
	@Column(name = "target_id", nullable = false)
	private UUID targetId;

	/**
	 * 作成日時（DB登録時に自動設定、更新不可）
	 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@PrePersist
	public void onPrePersist() {
		this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
	}
}
