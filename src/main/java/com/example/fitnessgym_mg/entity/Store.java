package com.example.fitnessgym_mg.entity;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"users", "customers"})
public class Store {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(unique = true, nullable = false, length = 100)
	private String name;

	@ManyToMany
	@JoinTable(name = "user_stores", // 中間テーブル名
			joinColumns = @JoinColumn(name = "store_id", nullable = false), // Store側のFK
			inverseJoinColumns = @JoinColumn(name = "user_id", nullable = false), // User側のFK
			uniqueConstraints = @UniqueConstraint(columnNames = { "store_id", "user_id" }) //複合ユニーク制約
	)
	private Set<User> users; // 店舗に所属するユーザーリスト

	@ManyToMany
	@JoinTable(name = "store_customers", // 中間テーブル名
			joinColumns = @JoinColumn(name = "store_id", nullable = false), // Store側のFK
			inverseJoinColumns = @JoinColumn(name = "customer_id", nullable = false), // Customer側のFK
			uniqueConstraints = @UniqueConstraint(columnNames = { "store_id", "customer_id" }) //複合ユニーク制約
	)
	private Set<Customer> customers; // 店舗に所属する顧客リスト

	@Override
	public int hashCode() {
		if (id == null) {
			return super.hashCode();
		}
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Store store = (Store) obj;
		return id != null && id.equals(store.id);
	}
}
