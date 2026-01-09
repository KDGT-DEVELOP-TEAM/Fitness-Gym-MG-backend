package com.example.fitnessgym_mg.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.fitnessgym_mg.entity.Customer;

/**
 * Customerリポジトリのカスタムインターフェース
 * 論理削除条件を自動的に適用するメソッドを提供
 */
public interface CustomerRepositoryCustom {

	/**
	 * 論理削除されていない顧客を検索（ページネーション対応）
	 * 
	 * <p>指定されたSpecificationに自動的に`notDeleted()`条件を適用します。
	 * これにより、論理削除条件の適用漏れを防止します。</p>
	 * 
	 * <p>重要: このメソッドを使用することで、論理削除条件が自動的に適用されます。
	 * 直接`findAll`を使用せず、必ずこのメソッドを使用してください。</p>
	 * 
	 * <p>パフォーマンスに関する注意事項:</p>
	 * <ul>
	 *   <li>Specificationが複雑になる場合、SQLが複雑化してパフォーマンスに影響する可能性があります。</li>
	 *   <li>大規模データでは、インデックス設計やクエリ計画を確認してください。</li>
	 *   <li>必要に応じて、DTO投影やカスタムネイティブクエリへの切り替えを検討してください。</li>
	 * </ul>
	 * 
	 * @param spec 検索条件（論理削除条件は自動的に追加される）
	 * @param pageable ページネーション情報
	 * @return 検索結果のページ
	 */
	Page<Customer> findAllNotDeleted(Specification<Customer> spec, Pageable pageable);

	/**
	 * 論理削除されていない全顧客を取得（オプション選択用）
	 * 
	 * <p>オプション選択用の全件取得メソッドです。
	 * 論理削除条件が自動的に適用されます。</p>
	 * 
	 * @return 論理削除されていない全顧客のリスト
	 */
	java.util.List<Customer> findAllNotDeleted();

	/**
	 * 全顧客のIDと名前を取得（オプション選択用、@SQLRestrictionを回避）
	 * 
	 * <p>ネイティブSQLクエリを使用して@SQLRestrictionを完全に回避します。
	 * オプション選択用なので、idとnameのみを取得します。</p>
	 * 
	 * @return 顧客のIDと名前のリスト（[id, name]の配列）
	 */
	java.util.List<Object[]> findAllIdAndNameForOptions();

	/**
	 * 顧客IDで顧客を取得し、storesもJOIN FETCHで一括取得（@SQLRestrictionを回避するため、ネイティブSQLクエリを使用）
	 * 
	 * <p>ネイティブSQLクエリを使用することで、Hibernateの`@SQLRestriction`の影響を完全に回避できます。</p>
	 * <p>データベースに`deleted_at`カラムが存在しない場合でも、エラーが発生しません。</p>
	 * 
	 * @param customerId 顧客ID
	 * @return 顧客エンティティ（存在しない場合はempty）
	 */
	java.util.Optional<Customer> findByIdWithStoresNative(java.util.UUID customerId);

	/**
	 * マネージャーと顧客が同じ店舗に所属しているか確認（@SQLRestrictionを回避するため、ネイティブSQLクエリを使用）
	 * 
	 * <p>ネイティブSQLクエリを使用することで、Hibernateの`@SQLRestriction`の影響を完全に回避できます。</p>
	 * 
	 * @param managerId マネージャーID
	 * @param customerId 顧客ID
	 * @return 同じ店舗に所属している場合 true
	 */
	boolean existsManagerCustomerInSameStoreNative(java.util.UUID managerId, java.util.UUID customerId);

	/**
	 * メールアドレスの存在確認（@SQLRestrictionを回避するため、ネイティブSQLクエリを使用）
	 * 
	 * <p>ネイティブSQLクエリを使用することで、Hibernateの`@SQLRestriction`の影響を完全に回避できます。</p>
	 * 
	 * @param email メールアドレス
	 * @return メールアドレスが存在する場合 true
	 */
	boolean existsByEmailNative(String email);

	/**
	 * メールアドレスの存在確認（指定ID以外、@SQLRestrictionを回避するため、ネイティブSQLクエリを使用）
	 * 
	 * <p>ネイティブSQLクエリを使用することで、Hibernateの`@SQLRestriction`の影響を完全に回避できます。</p>
	 * 
	 * @param email メールアドレス
	 * @param id 除外する顧客ID
	 * @return メールアドレスが存在する場合 true
	 */
	boolean existsByEmailAndIdNotNative(String email, java.util.UUID id);
}
