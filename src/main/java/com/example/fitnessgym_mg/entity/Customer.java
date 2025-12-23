package com.example.fitnessgym_mg.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * 顧客エンティティ
 * ジムの顧客情報を表す
 */
@Entity
@Table(name = "customers")
@DynamicUpdate
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "stores" })
public class Customer {

	@EqualsAndHashCode.Include
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/**
	 * フリガナ
	 */
	@Column(nullable = false, length = 100)
	private String kana;

	/**
	 * 顧客名
	 */
	@Column(nullable = false, length = 100)
	private String name;

	/**
	 * 性別
	 */
	@jakarta.persistence.Convert(converter = com.example.fitnessgym_mg.entity.converter.GenderConverter.class)
	@Column(nullable = false, columnDefinition = "customer_gender")
	private com.example.fitnessgym_mg.entity.enums.Gender gender;

	/**
	 * 生年月日
	 */
	@Column(nullable = false)
	private LocalDate birthday;

	/**
	 * 身長（cm）
	 */
	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal height;

	/**
	 * メールアドレス（ユニーク制約あり）
	 */
	@Column(unique = true, nullable = false, length = 255)
	private String email;

	/**
	 * 電話番号
	 */
	@Column(nullable = false, length = 12)
	private String phone;

	/**
	 * 住所
	 */
	@Column(nullable = false, length = 500)
	private String address;

	/**
	 * 医療・既往歴（任意）
	 */
	@Column(length = 500)
	private String medical;

	/**
	 * 禁忌事項（任意）
	 */
	@Column(length = 500)
	private String taboo;

	/**
	 * 初回姿勢画像ID（外部キー、新規作成時は任意）
	 */
	@Column(name = "first_posture_group_id")
	private UUID firstPostureGroupId;

	/**
	 * メモ（任意）
	 */
	@Column(length = 1000)
	private String memo;

	/**
	 * 作成日時（DB登録時に自動設定、更新不可）
	 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * 有効/無効フラグ
	 */
	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@PrePersist
	public void onPrePersist() {
		this.createdAt = LocalDateTime.now();
	}

	@ManyToMany
	@JoinTable(name = "store_customers", // 中間テーブル名
			joinColumns = @JoinColumn(name = "customer_id", nullable = false), // Customer側のFK
			inverseJoinColumns = @JoinColumn(name = "store_id", nullable = false), // Store側のFK
			uniqueConstraints = @UniqueConstraint(columnNames = { "customer_id", "store_id" }) //複合ユニーク制約
	)
	private Set<Store> stores; // 顧客が所属する店舗リスト
}