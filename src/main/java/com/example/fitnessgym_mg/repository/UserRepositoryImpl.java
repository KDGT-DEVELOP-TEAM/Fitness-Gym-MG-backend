package com.example.fitnessgym_mg.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

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
		Long total = ((Number) countQuery.getSingleResult()).longValue();

		return new PageImpl<>(users, pageable, total);
	}
}
