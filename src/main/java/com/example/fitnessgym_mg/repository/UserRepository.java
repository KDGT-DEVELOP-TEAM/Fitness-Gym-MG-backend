package com.example.fitnessgym_mg.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.User.UserRole;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

	/**
	 * メールアドレスでユーザーを検索（認証用）
	 */
	Optional<User> findByEmail(String email);

	/**
	 * メールアドレスでアクティブなユーザーを検索（認証用）
	 */
	Optional<User> findByEmailAndIsActiveTrue(String email);

	/**
	 * メールアドレスでユーザーを検索し、storesをJOIN FETCHで取得
	 * 店長のリダイレクト処理などで使用
	 */
	@Query("SELECT u FROM User u LEFT JOIN FETCH u.stores WHERE u.email = :email")
	Optional<User> findByEmailWithStores(@Param("email") String email);

	// keyword 単体検索（名前・かなに対して部分一致）
	@Query("""
			SELECT u FROM User u
			WHERE (:keyword IS NULL OR :keyword = ''
			       OR (u.name LIKE CONCAT('%', :keyword, '%')
			       OR u.kana LIKE CONCAT('%', :keyword, '%')))
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
			  AND (:role IS NULL OR u.role = :role)
			""")
	Page<User> findByKeywordAndRole(
			@Param("keyword") String keyword,
			@Param("role") UserRole role,
			Pageable pageable);

	// roleで検索
	Page<User> findByRole(UserRole role, Pageable sortedPageable);
}
