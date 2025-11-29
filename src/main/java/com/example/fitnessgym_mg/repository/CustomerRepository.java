package com.example.fitnessgym_mg.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.fitnessgym_mg.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {
	// keyword 単体検索（名前・かなに対して部分一致）
	@Query("""
			SELECT c FROM Customer c
			WHERE (:keyword IS NULL OR :keyword = ''
			       OR c.name LIKE CONCAT('%', :keyword, '%')
			       OR c.kana LIKE CONCAT('%', :keyword, '%'))
			""")
	Page<Customer> findByKeyword(
			@Param("keyword") String keyword,
			Pageable pageable);
}
