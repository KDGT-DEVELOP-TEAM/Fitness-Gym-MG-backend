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
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User>, UserRepositoryCustom {

	/**
	 * メールアドレスでユーザーを検索
	 * 
	 * <p>管理用途・データ確認用途で使用します。active条件は適用されません。</p>
	 * <p>認証用途の場合は、{@link #findByEmailAndActiveTrue(String)}を使用してください。</p>
	 * 
	 * @param email メールアドレス
	 * @return ユーザー（active条件なし）
	 */
	Optional<User> findByEmail(String email);

	/**
	 * メールアドレスでアクティブなユーザーを検索（認証用）
	 */
	Optional<User> findByEmailAndActiveTrue(String email);

	/**
	 * メールアドレスでユーザーを検索し、storesをJOIN FETCHで取得
	 * 店長のリダイレクト処理などで使用
	 * 
	 * <p>認証後の処理で使用するため、activeなユーザーのみ取得します。</p>
	 * 
	 * @param email メールアドレス
	 * @return ユーザー（active=trueの場合のみ）
	 */
	@Query("SELECT u FROM User u LEFT JOIN FETCH u.stores WHERE u.email = :email AND u.active = true")
	Optional<User> findByEmailWithStores(@Param("email") String email);

	/**
	 * キーワード（名前・かな）とロールでユーザーを検索
	 * 
	 * <p>LIKE句はCONCATを使用してSQLインジェクション対策を実施しています。</p>
	 * 
	 * <p>設計方針:</p>
	 * <ul>
	 *   <li>管理画面用の検索メソッドです。active=falseのユーザーも取得対象となります。</li>
	 *   <li>一般利用者向けの検索に使用する場合は、active条件を追加する必要があります。</li>
	 * </ul>
	 * 
	 * <p>注意: 先頭ワイルドカード（%keyword%）のため、インデックスが効きません。
	 * ユーザー数が増加した場合は、前方一致、正規化カラム、全文検索への切り替えを検討してください。</p>
	 * 
	 * <p>このメソッドは非推奨です。全文検索を使用する場合は{@link UserRepositoryCustom#searchByFullText(String, UserRole, Pageable)}を使用してください。</p>
	 * 
	 * @param keyword 検索キーワード（名前・かな）
	 * @param role ロール（nullの場合は全ロール）
	 * @param pageable ページネーション情報
	 * @return 検索結果のページ
	 * @deprecated 全文検索を使用する場合は{@link UserRepositoryCustom#searchByFullText(String, UserRole, Pageable)}を使用してください
	 */
	@Deprecated
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
