package com.example.fitnessgym_mg.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;

/**
 * ユーザーエンティティ用リポジトリ
 * ユーザーの検索、認証、ロール別検索などの機能を提供
 */
@Repository
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

	/**
	 * キーワード（名前・かな）とロールでユーザーを検索
	 * LIKE句はCONCATを使用してSQLインジェクション対策
	 */
	@Query("""
			SELECT u FROM User u
			WHERE (:keyword IS NULL OR :keyword = ''
			       OR u.name LIKE CONCAT('%', :keyword, '%')
			       OR u.kana LIKE CONCAT('%', :keyword, '%'))
			  AND (:role IS NULL OR u.role = :role)
			ORDER BY u.createdAt DESC
			""")
	Page<User> findByKeywordAndRole(
			@Param("keyword") String keyword,
			@Param("role") UserRole role,
			Pageable pageable);

	/**
	 * ロールでユーザーを検索
	 */
	Page<User> findByRole(UserRole role, Pageable pageable);
}
