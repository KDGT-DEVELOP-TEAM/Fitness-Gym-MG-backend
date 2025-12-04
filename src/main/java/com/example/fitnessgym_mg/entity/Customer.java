package com.example.fitnessgym_mg.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customers")
@DynamicUpdate
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String kana;

	@Column(nullable = false)
	private String name;

	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false, columnDefinition = "customer_gender")
	private CustomerGender gender;

	@Column(nullable = false)
	private LocalDate birthday;

	@Column(nullable = false)
	private Double height; // numeric型に対応

	@Column(unique = true, nullable = false)
	private String email;

	@Column(nullable = false, length = 12)
	private String phone;

	@Column(nullable = false)
	private String address;

	@Column
	private String medical; // 任意

	@Column
	private String taboo; // 任意

	// 初回姿勢画像ID (外部キーであり、新規作成時は任意)
	@Column(name = "first_posture_group_id")
	private UUID firstPostureGroupId;

	@Column
	private String memo; // 任意

	// 備考: 作成日時は、DB側でデフォルト値が設定されるため、Java側では変更不可 (updatable=false) とする
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@PrePersist
	public void onPrePersist() {
		this.createdAt = LocalDateTime.now();
	}

	public enum CustomerGender {
		男, 女;
	}

	@ManyToMany
	@JoinTable(name = "store_customers", // 中間テーブル名
			joinColumns = @JoinColumn(name = "customer_id", nullable = false), // Customer側のFK
			inverseJoinColumns = @JoinColumn(name = "store_id", nullable = false), // Store側のFK
			uniqueConstraints = @UniqueConstraint(columnNames = { "customer_id", "user_id" }) //複合ユニーク制約
	)
	private Set<Store> stores; // 顧客が所属する店舗リスト
}