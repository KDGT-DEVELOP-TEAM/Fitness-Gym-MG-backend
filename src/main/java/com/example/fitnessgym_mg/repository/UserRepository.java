package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.fitnessgym_mg.entity.User;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
	// keyword 検索 (name OR kana)
	List<User> findByNameContainingIgnoreCaseOrKanaContainingIgnoreCase(
			String nameKeyword, String kanaKeyword, Sort sort);

	// keyword + role
	List<User> findByNameContainingIgnoreCaseOrKanaContainingIgnoreCaseAndRole(
			String nameKeyword, String kanaKeyword, String role, Sort sort);
}