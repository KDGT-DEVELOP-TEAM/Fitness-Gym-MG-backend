package com.example.fitnessgym_mg.entity;

import java.time.LocalDateTime;
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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lessons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	// 実施時間
	@Column(name = "start_date", nullable = true) // NOT NULLではない
	private LocalDateTime startDate;

	@Column(name = "end_date", nullable = true) // NOT NULLではない
	private LocalDateTime endDate;

	// ------------------------------------
	// 必須リレーション
	// ------------------------------------

	// 実施店舗 (FK: store_id)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "store_id", nullable = false)
	private Store store;

	// 担当トレーナー (FK: user_id)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User trainer; // DBは user_id だが、Java側は trainer と命名

	// 顧客 (FK: customer_id)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	// 姿勢画像グループ (FK: posture_group_id)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "posture_group_id", nullable = true) // NOT NULLではない
	private PostureGroup postureGroup;

	// ------------------------------------
	// フィールド
	// ------------------------------------

	@Column(length = 50)
	private String condition; // 体調

	private Double weight; // 体重 (numeric は Double/BigDecimal)

	@Column(length = 150)
	private String meal; // 食事

	@Column(length = 500)
	private String memo; // 会話などのメモ

	// ------------------------------------
	// 次回予約関連
	// ------------------------------------

	// 次回のレッスン予約
	@Column(name = "next_date", nullable = true)
	private LocalDateTime nextDate;

	// 次回の実施店舗 (FK: next_store_id)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "next_store_id", nullable = true)
	private Store nextStore;

	// 次回の担当トレーナー (FK: next_user_id)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "next_user_id", nullable = true)
	private User nextUser;

	// ------------------------------------

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	public void onPrePersist() {
		this.createdAt = LocalDateTime.now();
	}
}