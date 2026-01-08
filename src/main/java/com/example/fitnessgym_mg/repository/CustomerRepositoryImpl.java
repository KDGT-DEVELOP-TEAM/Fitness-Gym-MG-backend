package com.example.fitnessgym_mg.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.repository.specification.CustomerSpecifications;

import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * Customerリポジトリのカスタム実装
 * 論理削除条件を自動的に適用する機能を提供
 * 
 * <p>注意: データベースのcustomersテーブルにdeleted_atカラムが存在しないため、
 * @SQLRestrictionを回避するためにネイティブSQLクエリを使用します。</p>
 */
@Repository
public class CustomerRepositoryImpl extends SimpleJpaRepository<Customer, java.util.UUID> 
		implements CustomerRepositoryCustom {
	
	private final EntityManager entityManager;
	
	public CustomerRepositoryImpl(EntityManager entityManager) {
		super(Customer.class, entityManager);
		this.entityManager = entityManager;
	}
	
	@Override
	public Page<Customer> findAllNotDeleted(Specification<Customer> spec, Pageable pageable) {
		// 重要: 論理削除条件を明示的に強制
		// notDeleted()を最初の条件として設定
		// これにより、論理削除条件の適用漏れを構造的に防止
		// 将来的にはユニットテストで論理削除条件の適用漏れを検証することを推奨
		Specification<Customer> notDeletedSpec = CustomerSpecifications.notDeleted();
		if (spec != null) {
			notDeletedSpec = notDeletedSpec.and(spec);
		}
		
		// SimpleJpaRepositoryのfindAllメソッドを使用
		return findAll(notDeletedSpec, pageable);
	}

	@Override
	public List<Customer> findAllNotDeleted() {
		// @SQLRestriction("deleted_at IS NULL")を回避するためにネイティブSQLクエリを使用
		// データベースのcustomersテーブルにdeleted_atカラムが存在しないため、
		// Hibernateの自動的な@SQLRestrictionの適用を回避する必要がある
		// ネイティブクエリを使用してCustomerエンティティを直接取得
		String nativeQuery = "SELECT * FROM customers";
		
		@SuppressWarnings("unchecked")
		List<Customer> results = entityManager.createNativeQuery(nativeQuery, Customer.class).getResultList();
		
		return results;
	}

	@Override
	public List<Object[]> findAllIdAndNameForOptions() {
		// @SQLRestriction("deleted_at IS NULL")を完全に回避するためにネイティブSQLクエリを使用
		// Object[]を返すことで、Hibernateのエンティティマッピングを完全に回避
		// オプション選択用なので、idとnameのみを取得
		String nativeQuery = "SELECT id, name FROM customers";
		
		@SuppressWarnings("unchecked")
		List<Object[]> results = entityManager.createNativeQuery(nativeQuery).getResultList();
		
		return results;
	}
}

