package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fitnessgym_mg.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
	// keyword 検索 (name OR kana)
	List<User> findByNameContainingIgnoreCaseOrKanaContainingIgnoreCase(
			String nameKeyword, String kanaKeyword);

	// keyword + role
	List<User> findByNameContainingIgnoreCaseOrKanaContainingIgnoreCaseAndRole(
			String nameKeyword, String kanaKeyword, String role);
}