package com.example.fitnessgym_mg.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Hibernate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;

/**
 * Userリポジトリのカスタム実装
 * 全文検索機能を実装
 */
@Repository
public class UserRepositoryImpl implements UserRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * 全文検索を使用したユーザー検索の実装
	 * 
	 * <p>PostgreSQLの全文検索機能を使用します。</p>
	 * <p>search_vectorカラム（tsvector型）が存在することを前提とします。</p>
	 */
	@Override
	public Page<User> searchByFullText(String keyword, UserRole role, UUID storeId, Pageable pageable) {
		// キーワードがnullまたは空文字の場合は、全文検索をスキップ
		boolean useFullTextSearch = keyword != null && !keyword.trim().isEmpty();

		// WHERE句の条件を構築（Listを使用して条件を管理し、可読性と保守性を向上）
		java.util.List<String> whereConditions = new java.util.ArrayList<>();
		java.util.List<String> joinClauses = new java.util.ArrayList<>();

		// --- 店舗IDによる絞り込み (中間テーブル user_stores 経由) ---
		if (storeId != null) {
			joinClauses.add("INNER JOIN user_stores us ON u.id = us.user_id");
			whereConditions.add("us.store_id = :storeId");
		}

		// --- 全文検索による絞り込み ---
		if (useFullTextSearch) {
			whereConditions.add("u.search_vector @@ plainto_tsquery('simple', :keyword)");
		}

		// --- ロールによる絞り込み ---
		if (role != null) {
			whereConditions.add("u.role = :role");
		}

		// SQLクエリを構築
		StringBuilder sql = new StringBuilder("SELECT DISTINCT u.* FROM users u");
		
		// JOIN句を追加
		for (String joinClause : joinClauses) {
			sql.append(" ").append(joinClause);
		}
		
		// WHERE句を構築（条件が1つ以上ある場合のみ）
		if (!whereConditions.isEmpty()) {
			sql.append(" WHERE ");
			sql.append(String.join(" AND ", whereConditions));
		}
		
		sql.append(" ORDER BY u.created_at DESC");

		// カウントクエリを構築（データ取得クエリと同じJOINとWHERE条件を使用）
		StringBuilder countSqlBuilder = new StringBuilder("SELECT COUNT(DISTINCT u.id) FROM users u");
		
		// JOIN句を追加（データ取得クエリと同じ）
		for (String joinClause : joinClauses) {
			countSqlBuilder.append(" ").append(joinClause);
		}
		
		// WHERE句を構築（データ取得クエリと同じ条件）
		if (!whereConditions.isEmpty()) {
			countSqlBuilder.append(" WHERE ");
			countSqlBuilder.append(String.join(" AND ", whereConditions));
		}
		
		String countSql = countSqlBuilder.toString();

		// データ取得クエリ
		Query query = entityManager.createNativeQuery(sql.toString(), User.class);
		Query countQuery = entityManager.createNativeQuery(countSql);

		if (storeId != null) {
			query.setParameter("storeId", storeId);
			countQuery.setParameter("storeId", storeId);
		}
		if (useFullTextSearch) {
			query.setParameter("keyword", keyword.trim());
			countQuery.setParameter("keyword", keyword.trim());
		}
		if (role != null) {
			query.setParameter("role", role.name());
			countQuery.setParameter("role", role.name());
		}

		query.setFirstResult((int) pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());

		@SuppressWarnings("unchecked")
		List<User> users = query.getResultList();

		// stores関係を明示的にロード（LazyInitializationExceptionを防ぐ）
		// ネイティブクエリで取得したエンティティは管理外の可能性があるため、
		// 別途storesを取得して設定する
		// 
		// 注意: この実装はN+1問題を回避するために、全ユーザーのstoresを一度のクエリで取得している。
		// 現状は適切な実装だが、将来的に大量のユーザーを取得する場合、メモリ使用量が増加する可能性がある。
		// パフォーマンス問題が発生した場合は、DTO Projectionへの切り替えを検討すること。
		if (!users.isEmpty()) {
			// 全ユーザーIDを取得
			List<UUID> userIds = users.stream().map(User::getId).toList();
			
			// 全ユーザーのstoresを一度のクエリで取得
			String storesQuery = """
					SELECT us.user_id, s.id, s.name
					FROM user_stores us
					JOIN stores s ON us.store_id = s.id
					WHERE us.user_id IN (:userIds)
					""";
			
			@SuppressWarnings("unchecked")
			List<Object[]> storeResults = entityManager
					.createNativeQuery(storesQuery)
					.setParameter("userIds", userIds)
					.getResultList();
			
			// User IDをキーとしたMapを作成（簡易的な実装）
			java.util.Map<UUID, Set<Store>> userStoresMap = new java.util.HashMap<>();
			for (Object[] row : storeResults) {
				try {
					UUID userId;
					if (row[0] instanceof UUID) {
						userId = (UUID) row[0];
					} else if (row[0] instanceof String) {
						userId = UUID.fromString((String) row[0]);
					} else {
						continue;
					}
					
					UUID fetchedStoreId;
					if (row[1] instanceof UUID) {
						fetchedStoreId = (UUID) row[1];
					} else if (row[1] instanceof String) {
						fetchedStoreId = UUID.fromString((String) row[1]);
					} else {
						continue;
					}
					
					String storeName = row[2] != null ? row[2].toString() : null;
					if (storeName == null) {
						continue;
					}
					
					Store store = new Store();
					store.setId(fetchedStoreId);
					store.setName(storeName);
					
					userStoresMap.computeIfAbsent(userId, k -> new HashSet<>()).add(store);
				} catch (Exception e) {
					// マッピングエラーはスキップ
					continue;
				}
			}
			
			// 各Userにstoresを設定
			users.forEach(user -> {
				Set<Store> stores = userStoresMap.getOrDefault(user.getId(), new HashSet<>());
				user.setStores(stores);
			});
		}

		Long total = ((Number) countQuery.getSingleResult()).longValue();

		return new PageImpl<>(users, pageable, total);
	}
}
