package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.fitnessgym_mg.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {
	// keyword 検索 (name OR kana)
	List<Customer> findByNameContainingIgnoreCaseOrKanaContainingIgnoreCase(
			String nameKeyword, String kanaKeyword, Sort sort);
}
