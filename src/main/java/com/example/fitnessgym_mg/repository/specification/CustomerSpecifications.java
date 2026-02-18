package com.example.fitnessgym_mg.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import com.example.fitnessgym_mg.entity.Customer;

/**
 * Customerエンティティ用の共通Specification
 * 論理削除条件などを一元管理
 */
public class CustomerSpecifications {

	/**
	 * 論理削除されていない顧客を取得するSpecification
	 * 
	 * <p>すべての取得系メソッドで必ず合成すること。</p>
	 * 
	 * @return 論理削除されていない顧客のSpecification（deleted_at IS NULL）
	 */
	public static Specification<Customer> notDeleted() {
		return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
	}
}
