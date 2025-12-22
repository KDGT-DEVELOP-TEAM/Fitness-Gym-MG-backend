package com.example.fitnessgym_mg.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.Customer;

/**
 * 顧客エンティティ用リポジトリ
 * 顧客の検索、キーワード検索などの機能を提供
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {
	
	/**
	 * キーワード（名前・かな）で顧客を検索
	 * LIKE句はCONCATを使用してSQLインジェクション対策
	 * 
	 * @param keyword 検索キーワード（名前またはかなの部分一致）
	 * @param pageable ページネーション情報
	 * @return 検索結果のページ
	 */
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
