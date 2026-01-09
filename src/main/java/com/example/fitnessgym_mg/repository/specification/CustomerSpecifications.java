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
	 * <p>注意: データベースにdeleted_atカラムが存在しないため、常にtrueを返します。</p>
	 * 
	 * @return 論理削除されていない顧客のSpecification（常にtrue）
	 */
	public static Specification<Customer> notDeleted() {
		// deletedAtフィールドが@Transientのため、常にtrueを返す
		return (root, query, cb) -> cb.conjunction();
	}
}
