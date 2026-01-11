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
	public Page<User> searchByFullText(String keyword, UserRole role, Pageable pageable) {
		// キーワードがnullまたは空文字の場合は、全文検索をスキップ
		boolean useFullTextSearch = keyword != null && !keyword.trim().isEmpty();

		// ネイティブクエリでtsvectorを使用
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT u.* FROM users u");

		StringBuilder whereClause = new StringBuilder();
		boolean hasCondition = false;

		if (useFullTextSearch) {
			whereClause.append(" WHERE u.search_vector @@ plainto_tsquery('simple', :keyword)");
			hasCondition = true;
		}

		if (role != null) {
			if (hasCondition) {
				whereClause.append(" AND u.role = :role");
			} else {
				whereClause.append(" WHERE u.role = :role");
				hasCondition = true;
			}
		}

		sql.append(whereClause);
		sql.append(" ORDER BY u.created_at DESC");

		// カウントクエリ
		String countSql = "SELECT COUNT(*) FROM users u" + whereClause.toString();

		// データ取得クエリ
		Query query = entityManager.createNativeQuery(sql.toString(), User.class);
		Query countQuery = entityManager.createNativeQuery(countSql);

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
					
					UUID storeId;
					if (row[1] instanceof UUID) {
						storeId = (UUID) row[1];
					} else if (row[1] instanceof String) {
						storeId = UUID.fromString((String) row[1]);
					} else {
						continue;
					}
					
					String storeName = row[2] != null ? row[2].toString() : null;
					if (storeName == null) {
						continue;
					}
					
					Store store = new Store();
					store.setId(storeId);
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
