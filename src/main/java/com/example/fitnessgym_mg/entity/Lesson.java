package com.example.fitnessgym_mg.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@DynamicUpdate
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "store", "trainer", "customer", "postureGroup", "nextStore", "nextUser" })
public class Lesson {

	@EqualsAndHashCode.Include
	@Id
	private UUID id = UUID.randomUUID();

	/**
	 * レッスン開始日時（必須）
	 */
	@Column(name = "start_date", nullable = false)
	private LocalDateTime startDate;

	/**
	 * レッスン終了日時（必須）
	 */
	@Column(name = "end_date", nullable = false)
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
	 * 体重を設定（30~300kgの範囲で検証）
	 * 
	 * @param weight 体重（kg）
	 * @throws IllegalArgumentException 範囲外の値の場合
	 */
	public void setWeight(BigDecimal weight) {
		if (weight != null && (weight.compareTo(BigDecimal.valueOf(30)) < 0
				|| weight.compareTo(BigDecimal.valueOf(300)) > 0)) {
			throw new IllegalArgumentException("体重は30~300kgの範囲で入力してください");
		}
		this.weight = weight;
	}

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
		this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
	}
}