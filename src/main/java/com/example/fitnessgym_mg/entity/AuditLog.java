package com.example.fitnessgym_mg.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 監査ログエンティティ
 * システム操作の記録を保持
 */
@Entity
@Table(name = "logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private String action;

	@Column(name = "target_table", nullable = false)
	private String targetTable;

	@Column(name = "target_id", nullable = false)
	private String targetId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@PrePersist
	public void onPrePersist() {
		this.createdAt = OffsetDateTime.now();
	}
}
