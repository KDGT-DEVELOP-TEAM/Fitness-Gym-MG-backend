package com.example.fitnessgym_mg.entity;

import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.DynamicUpdate;

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
@DynamicUpdate
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "users", "customers" })
public class Store {
	
	@EqualsAndHashCode.Include
	@Id
	private UUID id = UUID.randomUUID();

	/**
	 * 店舗名（ユニーク制約あり）
	 */
	@Column(unique = true, nullable = false, length = 100)
	private String name;

	/**
	 * 店舗名を設定（バリデーション付き）
	 * 
	 * @param name 店舗名
	 * @throws IllegalArgumentException 店舗名がnull、空文字、または100文字を超える場合
	 */
	public void setName(String name) {
		if (name == null || name.isBlank() || name.length() > 100) {
			throw new IllegalArgumentException("店舗名は必須で100文字以内です");
		}
		this.name = name;
	}

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

	/**
	 * ユーザーとの関連を追加（双方向関連を同期）
	 * 
	 * <p>注意: このメソッドはエンティティ層で双方向関連を同期します。
	 * JPAのベストプラクティスに従い、エンティティ層で双方向関連の整合性を保ちます。
	 * プロジェクト全体でこの方針を統一しています。</p>
	 * 
	 * @param user 追加するユーザー
	 */
	public void addUser(User user) {
		if (user == null) {
			return;
		}
		if (this.users == null) {
			this.users = new java.util.HashSet<>();
		}
		this.users.add(user);
		// 双方向関連を同期
		if (user.getStores() == null) {
			user.setStores(new java.util.HashSet<>());
		}
		user.getStores().add(this);
	}

	/**
	 * 顧客との関連を追加（双方向関連を同期）
	 * 
	 * <p>注意: このメソッドはエンティティ層で双方向関連を同期します。
	 * JPAのベストプラクティスに従い、エンティティ層で双方向関連の整合性を保ちます。
	 * プロジェクト全体でこの方針を統一しています。</p>
	 * 
	 * @param customer 追加する顧客
	 */
	public void addCustomer(Customer customer) {
		if (customer == null) {
			return;
		}
		if (this.customers == null) {
			this.customers = new java.util.HashSet<>();
		}
		this.customers.add(customer);
		// 双方向関連を同期
		customer.addStore(this);
	}
}
