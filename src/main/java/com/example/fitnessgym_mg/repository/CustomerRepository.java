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
			""")
	java.util.Optional<Customer> findByIdWithStores(@Param("customerId") UUID customerId);

	/**
	 * マネージャーと顧客が同じ店舗に所属しているか確認（存在確認専用クエリ）
	 * 
	 * <p>注意: このメソッドは@SQLRestrictionを回避するため、CustomerRepositoryCustomの
	 * existsManagerCustomerInSameStoreNative()を使用します。</p>
	 * 
	 * @deprecated @SQLRestrictionを回避するため、CustomerRepositoryCustom.existsManagerCustomerInSameStoreNative()を使用してください。
	 * 
	 * @param managerId マネージャーID
	 * @param customerId 顧客ID
	 * @return 同じ店舗に所属している場合 true
	 */
	@Deprecated
	default boolean existsManagerCustomerInSameStore(UUID managerId, UUID customerId) {
		if (this instanceof CustomerRepositoryCustom) {
			return ((CustomerRepositoryCustom) this).existsManagerCustomerInSameStoreNative(managerId, customerId);
		}
		throw new UnsupportedOperationException("CustomerRepositoryCustomの実装が必要です");
	}

	/**
	 * メールアドレスの存在確認
	 * 
	 * <p>注意: このメソッドは@SQLRestrictionを回避するため、CustomerRepositoryCustomの
	 * existsByEmailNative()を使用します。</p>
	 * 
	 * @deprecated @SQLRestrictionを回避するため、CustomerRepositoryCustom.existsByEmailNative()を使用してください。
	 * 
	 * @param email メールアドレス
	 * @return メールアドレスが存在する場合 true
	 */
	@Deprecated
	default boolean existsByEmail(String email) {
		if (this instanceof CustomerRepositoryCustom) {
			return ((CustomerRepositoryCustom) this).existsByEmailNative(email);
		}
		throw new UnsupportedOperationException("CustomerRepositoryCustomの実装が必要です");
	}

	/**
	 * メールアドレスの存在確認（指定ID以外）
	 * 更新時の重複チェック用
	 * 
	 * <p>注意: このメソッドは@SQLRestrictionを回避するため、CustomerRepositoryCustomの
	 * existsByEmailAndIdNotNative()を使用します。</p>
	 * 
	 * @deprecated @SQLRestrictionを回避するため、CustomerRepositoryCustom.existsByEmailAndIdNotNative()を使用してください。
	 * 
	 * @param email メールアドレス
	 * @param id 除外する顧客ID
	 * @return メールアドレスが存在する場合 true
	 */
	@Deprecated
	default boolean existsByEmailAndIdNot(String email, UUID id) {
		if (this instanceof CustomerRepositoryCustom) {
			return ((CustomerRepositoryCustom) this).existsByEmailAndIdNotNative(email, id);
		}
		throw new UnsupportedOperationException("CustomerRepositoryCustomの実装が必要です");
	}

}
