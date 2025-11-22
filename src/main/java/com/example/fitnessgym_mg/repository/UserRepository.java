package com.example.fitnessgym_mg.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.fitnessgym_mg.entity.User;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
	// keyword 単体検索（名前・かなに対して部分一致）
	@Query("""
			SELECT u FROM User u
			WHERE (:keyword IS NULL OR :keyword = ''
			       OR u.name LIKE %:keyword%
			       OR u.kana LIKE %:keyword%)
			""")
	Page<User> findByKeyword(
			@Param("keyword") String keyword,
			Pageable pageable);

	// keyword + role の組み合わせ検索
	@Query("""
			SELECT u FROM User u
			WHERE (:keyword IS NULL OR :keyword = ''
			       OR u.name LIKE %:keyword%
			       OR u.kana LIKE %:keyword%)
			  AND (:role IS NULL OR :role = '' OR u.role = :role)
			""")
	Page<User> findByKeywordAndRole(
			@Param("keyword") String keyword,
			@Param("role") String role,
			Pageable pageable);

	// roleで検索
	Page<User> findByRole(String role, Pageable sortedPageable);
}