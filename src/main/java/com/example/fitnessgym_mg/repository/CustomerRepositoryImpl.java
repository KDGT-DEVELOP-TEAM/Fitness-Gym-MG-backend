package com.example.fitnessgym_mg.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.converter.GenderConverter;
import com.example.fitnessgym_mg.repository.specification.CustomerSpecifications;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

/**
 * Customerリポジトリのカスタム実装
 * 論理削除条件を自動的に適用する機能を提供
 * 
 * <p>注意: データベースのcustomersテーブルにdeleted_atカラムが存在しないため、
 * @SQLRestrictionを回避するためにネイティブSQLクエリを使用します。</p>
 * 
 * <p>注意: Spring Data JPAの命名規則により、このクラスは自動的にCustomerRepositoryの実装として認識されます。
 * @Repositoryアノテーションは不要です（付けると独立したBeanとして登録され、競合が発生します）。
 * これはSimpleJpaRepositoryを継承しているためです。UserRepositoryImplとは異なり、@PersistenceContextを使用していないため、
 * @Repositoryアノテーションは不要です。</p>
 */
@Slf4j
public class CustomerRepositoryImpl extends SimpleJpaRepository<Customer, java.util.UUID> 
		implements CustomerRepositoryCustom {
	
	private final EntityManager entityManager;
	
	public CustomerRepositoryImpl(EntityManager entityManager) {
		super(Customer.class, entityManager);
		this.entityManager = entityManager;
	}
	
	@Override
	public Page<Customer> findAllNotDeleted(Specification<Customer> spec, Pageable pageable) {
		// 重要: 論理削除条件を明示的に強制
		// notDeleted()を最初の条件として設定
		// これにより、論理削除条件の適用漏れを構造的に防止
		// 将来的にはユニットテストで論理削除条件の適用漏れを検証することを推奨
		Specification<Customer> notDeletedSpec = CustomerSpecifications.notDeleted();
		if (spec != null) {
			notDeletedSpec = notDeletedSpec.and(spec);
		}
		
		// SimpleJpaRepositoryのfindAllメソッドを使用
		return findAll(notDeletedSpec, pageable);
	}

	@Override
	public List<Customer> findAllNotDeleted() {
		// @SQLRestriction("deleted_at IS NULL")を回避するためにネイティブSQLクエリを使用
		// ただし、論理削除条件は明示的に適用する必要がある
		// データベースのcustomersテーブルにdeleted_atカラムが存在しないため、
		// Hibernateの自動的な@SQLRestrictionの適用を回避する必要がある
		// ネイティブクエリを使用してCustomerエンティティを直接取得
		// 重要: 論理削除条件（WHERE deleted_at IS NULL）を必ず含めること
		String nativeQuery = "SELECT * FROM customers WHERE deleted_at IS NULL";
		
		@SuppressWarnings("unchecked")
		List<Customer> results = entityManager.createNativeQuery(nativeQuery, Customer.class).getResultList();
		
		return results;
	}

	@Override
	public List<Object[]> findAllIdAndNameForOptions() {
		// @SQLRestriction("deleted_at IS NULL")を完全に回避するためにネイティブSQLクエリを使用
		// Object[]を返すことで、Hibernateのエンティティマッピングを完全に回避
		// オプション選択用なので、idとnameのみを取得
		// 重要: 論理削除条件（WHERE deleted_at IS NULL）を必ず含めること
		String nativeQuery = "SELECT id, name FROM customers WHERE deleted_at IS NULL";
		
		@SuppressWarnings("unchecked")
		List<Object[]> results = entityManager.createNativeQuery(nativeQuery).getResultList();
		
		return results;
	}

	@Override
	public Optional<Customer> findByIdWithStoresNative(java.util.UUID customerId) {
		// @SQLRestriction("deleted_at IS NULL")を回避するためにネイティブSQLクエリを使用
		// Object[]として取得し、手動でCustomerエンティティを構築することで、
		// Hibernateのエンティティマッピングと@SQLRestrictionの適用を完全に回避
		// 
		// 注意: このメソッドは手動でエンティティマッピングを行っているため、保守性が低い。
		// 将来的には、DTO Projectionや専用のマッピングライブラリ（MapStructなど）の導入を検討すること。
		// または、@SQLRestrictionを回避する別の方法を検討すること。
		String customerQuery = """
				SELECT id, kana, name, gender, birthday, height, email, phone, address,
				       medical, taboo, first_posture_group_id, memo, created_at, is_active, deleted_at
				FROM customers
				WHERE id = :customerId
				  AND deleted_at IS NULL
				""";
		
		@SuppressWarnings("unchecked")
		List<Object[]> customerResults = entityManager
				.createNativeQuery(customerQuery)
				.setParameter("customerId", customerId)
				.getResultList();
		
		if (customerResults.isEmpty()) {
			return Optional.empty();
		}
		
		Object[] row = customerResults.get(0);
		Customer customer = new Customer();
		
		// GenderConverterを使用してgenderを変換
		GenderConverter genderConverter = new GenderConverter();
		
		try {
			customer.setId(row[0] instanceof UUID ? (UUID) row[0] : UUID.fromString(row[0].toString()));
			customer.setKana(row[1] != null ? row[1].toString() : null);
			customer.setName(row[2] != null ? row[2].toString() : null);
			
			// gender: String型として取得し、GenderConverterで変換
			if (row[3] != null) {
				customer.setGender(genderConverter.convertToEntityAttribute(row[3].toString()));
			}
			
			// birthday: 様々な型に対応
			if (row[4] != null) {
				if (row[4] instanceof java.sql.Date) {
					customer.setBirthday(((java.sql.Date) row[4]).toLocalDate());
				} else if (row[4] instanceof java.time.LocalDate) {
					customer.setBirthday((java.time.LocalDate) row[4]);
				} else if (row[4] instanceof java.sql.Timestamp) {
					customer.setBirthday(((java.sql.Timestamp) row[4]).toLocalDateTime().toLocalDate());
				} else if (row[4] instanceof java.time.LocalDateTime) {
					customer.setBirthday(((java.time.LocalDateTime) row[4]).toLocalDate());
				}
			}
			
			customer.setHeight(row[5] != null && row[5] instanceof java.math.BigDecimal 
					? (java.math.BigDecimal) row[5] 
					: row[5] != null ? new java.math.BigDecimal(row[5].toString()) : null);
			customer.setEmail(row[6] != null ? row[6].toString() : null);
			customer.setPhone(row[7] != null ? row[7].toString() : null);
			customer.setAddress(row[8] != null ? row[8].toString() : null);
			customer.setMedical(row[9] != null ? row[9].toString() : null);
			customer.setTaboo(row[10] != null ? row[10].toString() : null);
			
			if (row[11] != null) {
				customer.setFirstPostureGroupId(row[11] instanceof UUID 
						? (UUID) row[11] 
						: UUID.fromString(row[11].toString()));
			}
			
			customer.setMemo(row[12] != null ? row[12].toString() : null);
			
			// created_at: 様々な型に対応
			if (row[13] != null) {
				if (row[13] instanceof java.sql.Timestamp) {
					customer.setCreatedAt(((java.sql.Timestamp) row[13]).toLocalDateTime());
				} else if (row[13] instanceof java.time.LocalDateTime) {
					customer.setCreatedAt((java.time.LocalDateTime) row[13]);
				}
			}
			
			// is_active: PostgreSQLのboolean型をBooleanオブジェクトとして取得
			// 様々な型に対応する堅牢な型変換を実装
			boolean isActive = false;
			if (row[14] != null) {
				if (row[14] instanceof Boolean) {
					isActive = (Boolean) row[14];
				} else if (row[14] instanceof Number) {
					// 数値型の場合（0=false, 1=true）
					isActive = ((Number) row[14]).intValue() != 0;
				} else if (row[14] instanceof String) {
					// 文字列型の場合（"true"/"false"）
					isActive = Boolean.parseBoolean(row[14].toString());
				}
			}
			customer.setActive(isActive);
			
			// deleted_at: OffsetDateTime型として取得
			if (row[15] != null) {
				if (row[15] instanceof java.sql.Timestamp) {
					customer.setDeletedAt(((java.sql.Timestamp) row[15]).toInstant()
						.atOffset(java.time.ZoneOffset.UTC));
				} else if (row[15] instanceof java.time.OffsetDateTime) {
					customer.setDeletedAt((java.time.OffsetDateTime) row[15]);
				} else if (row[15] instanceof java.time.ZonedDateTime) {
					customer.setDeletedAt(((java.time.ZonedDateTime) row[15]).toOffsetDateTime());
				}
			}
			
			// version はデータベースに存在しないため、null のままにする
		} catch (Exception mappingException) {
			throw new com.example.fitnessgym_mg.exception.SystemException(
				"Customerエンティティのマッピングに失敗しました: customerId=" + customerId, mappingException);
		}
		
		// storesを別途ネイティブSQLクエリで取得
		String storesQuery = """
				SELECT s.id, s.name
				FROM stores s
				JOIN store_customers sc ON s.id = sc.store_id
				WHERE sc.customer_id = :customerId
				""";
		
		@SuppressWarnings("unchecked")
		List<Object[]> storeResults = entityManager
				.createNativeQuery(storesQuery)
				.setParameter("customerId", customerId)
				.getResultList();
		
		// Storeエンティティを構築してCustomerに設定
		Set<Store> stores = new HashSet<>();
		for (Object[] storeRow : storeResults) {
			try {
				UUID storeId;
				if (storeRow[0] instanceof UUID) {
					storeId = (UUID) storeRow[0];
				} else if (storeRow[0] instanceof String) {
					storeId = UUID.fromString((String) storeRow[0]);
				} else {
					continue; // 型が予期しない場合はスキップ
				}
				
				String storeName = storeRow[1] != null ? storeRow[1].toString() : null;
				if (storeName == null) {
					continue;
				}
				
				Store store = new Store();
				store.setId(storeId);
				store.setName(storeName);
				stores.add(store);
			} catch (Exception storeMappingException) {
				// Storeのマッピングエラーはスキップして続行
				continue;
			}
		}
		
		customer.setStores(stores);
		
		return Optional.of(customer);
	}

	@Override
	public boolean existsManagerCustomerInSameStoreNative(UUID managerId, UUID customerId) {
		// @SQLRestriction("deleted_at IS NULL")を回避するためにネイティブSQLクエリを使用
		// マネージャーと顧客が同じ店舗に所属しているか確認
		String query = """
				SELECT EXISTS (
					SELECT 1
					FROM users u
					JOIN user_stores us ON u.id = us.user_id
					JOIN store_customers sc ON us.store_id = sc.store_id
					WHERE u.id = :managerId
					  AND sc.customer_id = :customerId
				)
				""";
		
		try {
			Object result = entityManager
					.createNativeQuery(query)
					.setParameter("managerId", managerId)
					.setParameter("customerId", customerId)
					.getSingleResult();
			
		// PostgreSQLではEXISTSの結果はboolean型だが、JPAのcreateNativeQueryでは様々な型で返される可能性がある
		if (result instanceof Boolean) {
			return (Boolean) result;
		} else if (result instanceof Number) {
			return ((Number) result).intValue() != 0;
		} else if (result instanceof String) {
			String str = ((String) result).trim().toLowerCase();
			return "true".equals(str) || "t".equals(str) || "1".equals(str);
		}
		// 予期しない型の場合は警告ログを出力してfalseを返す
		// これにより、データベースの変更や設定ミスを検出できる
		log.warn("existsManagerCustomerInSameStoreNative returned unexpected type: {}", 
			result != null ? result.getClass().getName() : "null");
		return false;
	} catch (jakarta.persistence.NoResultException e) {
		return false;
	} catch (Exception e) {
		// エラーをログに記録（スタックトレースは自動出力される）
		log.error("Error in existsManagerCustomerInSameStoreNative: managerId={}, customerId={}", 
			managerId, customerId, e);
		return false;
	}
	}

	@Override
	public boolean existsTrainerCustomerInSameStoreNative(UUID trainerId, UUID customerId) {
		// @SQLRestriction("deleted_at IS NULL")を回避するためにネイティブSQLクエリを使用
		// トレーナーと顧客が同じ店舗に所属しているか確認
		String query = """
				SELECT EXISTS (
					SELECT 1
					FROM users u
					JOIN user_stores us ON u.id = us.user_id
					JOIN store_customers sc ON us.store_id = sc.store_id
					WHERE u.id = :trainerId
					  AND sc.customer_id = :customerId
				)
				""";
		
		try {
			Object result = entityManager
					.createNativeQuery(query)
					.setParameter("trainerId", trainerId)
					.setParameter("customerId", customerId)
					.getSingleResult();
			
		// PostgreSQLではEXISTSの結果はboolean型だが、JPAのcreateNativeQueryでは様々な型で返される可能性がある
		if (result instanceof Boolean) {
			return (Boolean) result;
		} else if (result instanceof Number) {
			return ((Number) result).intValue() != 0;
		} else if (result instanceof String) {
			String str = ((String) result).trim().toLowerCase();
			return "true".equals(str) || "t".equals(str) || "1".equals(str);
		}
		// 予期しない型の場合は警告ログを出力してfalseを返す
		// これにより、データベースの変更や設定ミスを検出できる
		log.warn("existsTrainerCustomerInSameStoreNative returned unexpected type: {}", 
			result != null ? result.getClass().getName() : "null");
		return false;
	} catch (jakarta.persistence.NoResultException e) {
		return false;
	} catch (Exception e) {
		// エラーをログに記録（スタックトレースは自動出力される）
		log.error("Error in existsTrainerCustomerInSameStoreNative: trainerId={}, customerId={}", 
			trainerId, customerId, e);
		return false;
	}
	}

	@Override
	public boolean existsByEmailNative(String email) {
		// @SQLRestriction("deleted_at IS NULL")を回避するためにネイティブSQLクエリを使用
		// メールアドレスの存在確認
		// 重要: 論理削除条件（AND deleted_at IS NULL）を必ず含めること
		String query = "SELECT EXISTS (SELECT 1 FROM customers WHERE email = :email AND deleted_at IS NULL)";
		
		try {
			Object result = entityManager
					.createNativeQuery(query)
					.setParameter("email", email)
					.getSingleResult();
			
			if (result instanceof Boolean) {
				return (Boolean) result;
			} else if (result instanceof Number) {
				return ((Number) result).intValue() != 0;
			} else if (result instanceof String) {
				return Boolean.parseBoolean((String) result);
			}
			return false;
		} catch (jakarta.persistence.NoResultException e) {
			return false;
		}
	}

	@Override
	public boolean existsByEmailAndIdNotNative(String email, UUID id) {
		// @SQLRestriction("deleted_at IS NULL")を回避するためにネイティブSQLクエリを使用
		// メールアドレスの存在確認（指定ID以外）
		// 重要: 論理削除条件（AND deleted_at IS NULL）を必ず含めること
		String query = "SELECT EXISTS (SELECT 1 FROM customers WHERE email = :email AND id != :id AND deleted_at IS NULL)";
		
		try {
			Object result = entityManager
					.createNativeQuery(query)
					.setParameter("email", email)
					.setParameter("id", id)
					.getSingleResult();
			
			if (result instanceof Boolean) {
				return (Boolean) result;
			} else if (result instanceof Number) {
				return ((Number) result).intValue() != 0;
			} else if (result instanceof String) {
				return Boolean.parseBoolean((String) result);
			}
			return false;
		} catch (jakarta.persistence.NoResultException e) {
			return false;
		}
	}
}

