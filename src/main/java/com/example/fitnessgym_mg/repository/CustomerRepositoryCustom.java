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
}
