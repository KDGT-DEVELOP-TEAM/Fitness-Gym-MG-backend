package com.example.fitnessgym_mg.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.repository.specification.CustomerSpecifications;

import jakarta.persistence.EntityManager;

/**
 * Customerリポジトリのカスタム実装
 * 論理削除条件を自動的に適用する機能を提供
 */
@Repository
public class CustomerRepositoryImpl extends SimpleJpaRepository<Customer, java.util.UUID> 
		implements CustomerRepositoryCustom {
	
	public CustomerRepositoryImpl(EntityManager entityManager) {
		super(Customer.class, entityManager);
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
	public java.util.List<Customer> findAllNotDeleted() {
		Specification<Customer> notDeletedSpec = CustomerSpecifications.notDeleted();
		return findAll(notDeletedSpec);
	}
}

