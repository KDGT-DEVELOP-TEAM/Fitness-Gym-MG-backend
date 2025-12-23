package com.example.fitnessgym_mg.entity;

import java.math.BigDecimal;
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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * レッスンエンティティ
 * ジムのレッスン情報を表す
 */
@Entity
@Table(name = "lessons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "store", "trainer", "customer", "postureGroup", "nextStore", "nextUser" })
public class Lesson {

	@EqualsAndHashCode.Include
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/**
	 * レッスン開始日時
	 */
	@Column(name = "start_date", nullable = true)
	private LocalDateTime startDate;

	/**
	 * レッスン終了日時
	 */
	@Column(name = "end_date", nullable = true)
	private LocalDateTime endDate;

	/**
	 * 実施店舗（必須リレーション）
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "store_id", nullable = false)
	private Store store;

	/**
	 * 担当トレーナー（必須リレーション、DBはuser_idだがJava側はtrainerと命名）
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User trainer;

	/**
	 * 顧客（必須リレーション）
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	/**
	 * 姿勢画像グループ（任意リレーション）
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "posture_group_id", nullable = true)
	private PostureGroup postureGroup;

	/**
	 * 体調
	 */
	@Column(length = 500)
	private String condition;

	/**
	 * 体重（numeric型に対応）
	 */
	@Column(precision = 5, scale = 2)
	private BigDecimal weight;

	/**
	 * 食事内容
	 */
	@Column(length = 500)
	private String meal;

	/**
	 * メモ（会話などの記録）
	 */
	@Column(length = 500)
	private String memo;

	/**
	 * 次回レッスン予約日時
	 */
	@Column(name = "next_date", nullable = true)
	private LocalDateTime nextDate;

	/**
	 * 次回実施店舗（任意リレーション）
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "next_store_id", nullable = true)
	private Store nextStore;

	/**
	 * 次回担当トレーナー（任意リレーション）
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "next_user_id", nullable = true)
	private User nextUser;

	/**
	 * 作成日時（DB登録時に自動設定、更新不可）
	 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	public void onPrePersist() {
		this.createdAt = LocalDateTime.now();
	}
}