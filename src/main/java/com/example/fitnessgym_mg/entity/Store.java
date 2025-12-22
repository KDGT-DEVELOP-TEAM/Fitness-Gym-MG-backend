package com.example.fitnessgym_mg.entity;

import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 店舗エンティティ
 * ジムの店舗情報を表す
 */
@Entity
@Table(name = "stores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "users", "customers" })
public class Store {
	
	@EqualsAndHashCode.Include
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/**
	 * 店舗名（ユニーク制約あり）
	 */
	@Column(unique = true, nullable = false, length = 100)
	private String name;

	/**
	 * 店舗に所属するユーザーリスト（多対多リレーション）
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_stores", // 中間テーブル名
			joinColumns = @JoinColumn(name = "store_id", nullable = false), // Store側のFK
			inverseJoinColumns = @JoinColumn(name = "user_id", nullable = false), // User側のFK
			uniqueConstraints = @UniqueConstraint(columnNames = { "store_id", "user_id" }) //複合ユニーク制約
	)
	private Set<User> users;

	/**
	 * 店舗に所属する顧客リスト（多対多リレーション）
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "store_customers", // 中間テーブル名
			joinColumns = @JoinColumn(name = "store_id", nullable = false), // Store側のFK
			inverseJoinColumns = @JoinColumn(name = "customer_id", nullable = false), // Customer側のFK
			uniqueConstraints = @UniqueConstraint(columnNames = { "store_id", "customer_id" }) //複合ユニーク制約
	)
	private Set<Customer> customers;
}
