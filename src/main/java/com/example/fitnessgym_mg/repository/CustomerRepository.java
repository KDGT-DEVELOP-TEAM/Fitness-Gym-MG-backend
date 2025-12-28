package com.example.fitnessgym_mg.repository;

import java.util.UUID;

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
public interface CustomerRepository
		extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer>, CustomerRepositoryCustom {

	/**
	 * 顧客IDで顧客を取得し、storesもJOIN FETCHで一括取得
	 * パフォーマンス最適化のため、storesの遅延読み込みを回避
	 * 
	 * <p>注意事項:</p>
	 * <ul>
	 *   <li>最新レッスンの体重は別途取得が必要（CustomerエンティティにLessonへの直接リレーションがないため）</li>
	 *   <li>論理削除された顧客は除外される</li>
	 *   <li>storesの件数が多い場合、メモリ負荷が大きくなる可能性があります。将来的にDTOプロジェクションへの変更を検討してください。</li>
	 * </ul>
	 */
	@Query("""
			SELECT DISTINCT c
			FROM Customer c
			LEFT JOIN FETCH c.stores
			WHERE c.id = :customerId
			  AND c.deletedAt IS NULL
			""")
	java.util.Optional<Customer> findByIdWithStores(@Param("customerId") UUID customerId);

	/**
	 * マネージャーと顧客が同じ店舗に所属しているか確認（存在確認専用クエリ）
	 * 
	 * <p>N+1問題を回避するため、JOINを使用したEXISTSクエリで実装。</p>
	 * <p>EXISTSを使用することで、COUNTベースのクエリよりも効率的に動作します。</p>
	 * 
	 * @param managerId マネージャーID
	 * @param customerId 顧客ID
	 * @return 同じ店舗に所属している場合 true
	 */
	@Query("""
			SELECT CASE WHEN EXISTS (
				SELECT 1
				FROM User u
				JOIN u.stores ms
				JOIN Customer c ON c.id = :customerId
				JOIN c.stores cs
				WHERE u.id = :managerId
				  AND ms.id = cs.id
			) THEN true ELSE false END
			""")
	boolean existsManagerCustomerInSameStore(
			@Param("managerId") UUID managerId,
			@Param("customerId") UUID customerId);

	/**
	 * メールアドレスの存在確認（論理削除された顧客は除外）
	 * 
	 * @param email メールアドレス
	 * @return メールアドレスが存在する場合 true
	 */
	@Query("""
			SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
			FROM Customer c
			WHERE c.email = :email
			  AND c.deletedAt IS NULL
			""")
	boolean existsByEmail(@Param("email") String email);

	/**
	 * メールアドレスの存在確認（指定ID以外、論理削除された顧客は除外）
	 * 更新時の重複チェック用
	 * 
	 * @param email メールアドレス
	 * @param id 除外する顧客ID
	 * @return メールアドレスが存在する場合 true
	 */
	@Query("""
			SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
			FROM Customer c
			WHERE c.email = :email
			  AND c.id != :id
			  AND c.deletedAt IS NULL
			""")
	boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") UUID id);
}
