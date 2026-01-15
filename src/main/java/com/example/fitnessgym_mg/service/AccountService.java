package com.example.fitnessgym_mg.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors; // StoreエンティティのSetに変換するために追加

import jakarta.persistence.criteria.JoinType;

import org.hibernate.Hibernate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.request.UserRequest;
import com.example.fitnessgym_mg.dto.response.UserResponse;
import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import com.example.fitnessgym_mg.entity.enums.UserSortType;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.repository.StoreRepository;
import com.example.fitnessgym_mg.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

	private final UserRepository userRepository;
	private final StoreRepository storeRepository;
	private final PasswordEncoder passwordEncoder;
	private final LessonRepository lessonRepository;
	private final SupabaseAuthService supabaseAuthService;
	private final CustomerRepository customerRepository;

	@PersistenceContext
	private EntityManager entityManager;

	// --- ユーザー検索 ---
	@Transactional(readOnly = true)
	public Page<UserResponse> searchUsers(
			String keyword,
			UserRole role,
			UserSortType sort,
			UUID storeId, // 検索条件のstoreIdは単一でOK
			Pageable pageable) {

		log.debug("searchUsers called: keyword={}, role={}, sort={}, storeId={}, page={}, size={}", keyword, role, sort, storeId, pageable.getPageNumber(), pageable.getPageSize());
		Specification<User> spec = (root, query, cb) -> null;

		// --- 1-1. 店舗IDによる絞り込み (中間テーブル user_stores 経由) ---
		if (storeId != null) {
			spec = spec.and((root, query, cb) -> {
				var userStoresJoin = root.join("stores", jakarta.persistence.criteria.JoinType.INNER);
				return cb.equal(userStoresJoin.get("id"), storeId);
			});
		}

		// --- 1-2. キーワードによる絞り込み ---
		if (keyword != null && !keyword.isEmpty()) {
			// 全文検索を使用（高速）
			// ソートオブジェクトの生成 (グルーピングソート対応)
			Sort sortObj = createSort(sort);
			Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortObj);
			
			// 全文検索を実行（storeIdを渡すことで、店舗フィルタリングとADMINユーザー除外が行われる）
			Page<User> userPage = userRepository.searchByFullText(keyword, role, storeId, sortedPageable);
			
			// Managerからのアクセスの場合、ADMINユーザーを除外（念のため、店舗フィルタリングは既にSQLレベルで実装済み）
			if (storeId != null) {
				List<User> filteredContent = userPage.getContent().stream()
					.filter(user -> {
						// ADMINユーザーを除外
						if (user.getRole() == com.example.fitnessgym_mg.entity.enums.UserRole.ADMIN) {
							return false;
						}
						// 念のため、指定された店舗に所属するユーザーのみを返す（SQLレベルで既にフィルタリング済みだが、二重チェック）
						if (user.getStores() != null) {
							return user.getStores().stream()
								.anyMatch(store -> store.getId().equals(storeId));
						}
						return false;
					})
					.collect(java.util.stream.Collectors.toList());
				
				// フィルタリング後のページネーション情報を再計算
				long totalElements = filteredContent.size();
				
				return new org.springframework.data.domain.PageImpl<>(filteredContent, sortedPageable, totalElements)
					.map(UserResponse::fromEntity);
			}
			
			return userPage.map(UserResponse::fromEntity);
		}

		// --- 1-3. ロールによる絞り込み ---
		if (role != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
		}

		// --- 1-4. Managerからのアクセスの場合、ADMINユーザーを除外 ---
		if (storeId != null) {
			// storeIdが指定されている場合、これはManagerからのアクセス
			// ADMINロールのユーザーを除外する
			spec = spec.and((root, query, cb) -> cb.notEqual(root.get("role"), com.example.fitnessgym_mg.entity.enums.UserRole.ADMIN));
		}

		// 2. ソートオブジェクトの生成 (グルーピングソート対応)
		Sort sortObj = createSort(sort);

		Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortObj);

		// 3. 検索の実行（キーワードがない場合は従来通りSpecificationを使用）
		Page<User> users = userRepository.findAll(spec, sortedPageable);
		log.debug("searchUsers result (Specification): totalElements={}, totalPages={}, numberOfElements={}", users.getTotalElements(), users.getTotalPages(), users.getNumberOfElements());

		// stores関係を明示的にロード（LazyInitializationExceptionを防ぐ）
		// Hibernate.initialize()が機能しない場合に備えて、別途storesを取得して設定する
		List<User> userList = users.getContent();
		if (!userList.isEmpty()) {
			// 全ユーザーIDを取得
			List<UUID> userIds = userList.stream().map(User::getId).toList();
			
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
			
			// User IDをキーとしたMapを作成
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
					
					UUID currentStoreId;
					if (row[1] instanceof UUID) {
						currentStoreId = (UUID) row[1];
					} else if (row[1] instanceof String) {
						currentStoreId = UUID.fromString((String) row[1]);
					} else {
						continue;
					}
					
					String storeName = row[2] != null ? row[2].toString() : null;
					if (storeName == null) {
						continue;
					}
					
					Store store = new Store();
					store.setId(currentStoreId);
					store.setName(storeName);
					
					userStoresMap.computeIfAbsent(userId, k -> new HashSet<>()).add(store);
				} catch (Exception e) {
					// マッピングエラーはスキップ
					continue;
				}
			}
			
			// 各Userにstoresを設定
			userList.forEach(user -> {
				Set<Store> stores = userStoresMap.getOrDefault(user.getId(), new HashSet<>());
				user.setStores(stores);
			});
		}

		return users.map(UserResponse::fromEntity);
	}

	// --- Admin用: ユーザー作成 ---
	@Transactional
	public void createByAdmin(UserRequest req, Set<UUID> storeIds) {
		// 店舗IDの正規化
		if (storeIds == null) {
			storeIds = Collections.emptySet();
		}
		
		if (req.getRole() == UserRole.TRAINER) {
			// トレーナーの場合、店舗は1つ以上必須
			if (storeIds == null || storeIds.isEmpty()) {
				throw new com.example.fitnessgym_mg.exception.BusinessRuleViolationException("トレーナーユーザーには、割り当てる店舗を1つ以上選択する必要があります。");
			}
		} else if (req.getRole() == UserRole.MANAGER) {
			// 店長ロールのバリデーション (単一の店舗必須)
			validateManagerRole(req.getRole(), storeIds);
		} else {
			// ADMINの場合、店舗は不要
			storeIds = Collections.emptySet();
		}

		// 共通の作成ロジック
		validateAndCreateUser(req, storeIds);
	}

	// --- Manager用: ユーザー作成 ---
	@Transactional
	public void createByManager(UserRequest req, UUID storeId) {
		// storeIdを強制追加
		Set<UUID> storeIds = new java.util.HashSet<>();
		storeIds.add(storeId);

		// Manager APIではTRAINERのみ作成可能なので、店舗IDの正規化は不要
		// storeIdsは既に1つのstoreIdが設定されている

		// 共通の作成ロジック
		validateAndCreateUser(req, storeIds);
	}

	// --- ユーザー作成の共通ロジック ---
	private void validateAndCreateUser(UserRequest req, Set<UUID> storeIds) {
		// DTO層で@Validによりバリデーション済みだが、Service層でも最終チェック
		// パスワードの必須チェック（新規作成時のみ）
		if (req.getPass() == null || req.getPass().trim().isEmpty()) {
			throw new com.example.fitnessgym_mg.exception.InvalidRequestException("パスワードは必須です。");
		}
		
		// パスワードの強度チェック
		if (req.getPass().length() < 6) {
			throw new com.example.fitnessgym_mg.exception.InvalidRequestException("パスワードは6文字以上である必要があります");
		}

		// メールアドレスの正規化
		String normalizedEmail = req.getEmail().trim().toLowerCase();
		log.info("Starting user creation process: email={}", normalizedEmail);
		
		// メールアドレスの重複チェック（ローカルDB - Userテーブル）
		boolean userExists = userRepository.findByEmail(normalizedEmail).isPresent();
		log.info("User table check: email={}, exists={}", normalizedEmail, userExists);
		if (userExists) {
			log.warn("Email already exists in User table: email={}", normalizedEmail);
			throw new com.example.fitnessgym_mg.exception.ConflictException("このメールアドレスは既に登録されています");
		}
		
		// Customerテーブルのメールアドレス重複チェック
		boolean customerExists = customerRepository.existsByEmailNative(normalizedEmail);
		log.info("Customer table check: email={}, exists={}", normalizedEmail, customerExists);
		if (customerExists) {
			log.warn("Email already exists in Customer table: email={}", normalizedEmail);
			throw new com.example.fitnessgym_mg.exception.ConflictException(
				"このメールアドレスは既に顧客として登録されています。顧客とユーザーで同じメールアドレスは使用できません。");
		}

		// Supabase Auth側のユーザー存在チェック（オプション、エラーが発生しても続行）
		boolean supabaseUserExists = false;
		try {
			log.info("Checking user existence in Supabase Auth: email={}", normalizedEmail);
			supabaseUserExists = supabaseAuthService.userExists(normalizedEmail);
			log.info("Supabase Auth existence check: email={}, exists={}", normalizedEmail, supabaseUserExists);
			if (supabaseUserExists) {
				log.warn("User already exists in Supabase Auth but not in local DB: email={}", normalizedEmail);
				throw new com.example.fitnessgym_mg.exception.ConflictException(
					"このメールアドレスは既にSupabase Authに登録されています。管理者に連絡してください。");
			}
		} catch (com.example.fitnessgym_mg.exception.ConflictException e) {
			// 既存ユーザーエラーはそのまま再スロー
			throw e;
		} catch (Exception e) {
			// 存在チェックのエラーは無視して続行（Supabase側でエラーになる可能性があるが、試行する）
			log.warn("Failed to check user existence in Supabase Auth: email={}, error={}", 
				normalizedEmail, e.getMessage());
		}

		// Supabase Authにユーザーを作成
		log.info("Attempting to create user in Supabase Auth: email={}", normalizedEmail);
		UUID authUserId;
		try {
			authUserId = supabaseAuthService.createUser(normalizedEmail, req.getPass());
			log.info("Successfully created user in Supabase Auth: email={}, authUserId={}", normalizedEmail, authUserId);
		} catch (IllegalArgumentException e) {
			// バリデーションエラー
			log.error("Invalid request for Supabase Auth user creation: email={}, error={}, userExists={}, customerExists={}", 
				normalizedEmail, e.getMessage(), userExists, customerExists, e);
			throw new com.example.fitnessgym_mg.exception.InvalidRequestException(e.getMessage(), e);
		} catch (RuntimeException e) {
			// 既存ユーザーエラーの場合
			if (e.getMessage() != null && e.getMessage().contains("既にSupabase Authに登録されています")) {
				log.warn("User already exists in Supabase Auth: email={}, userExists={}, customerExists={}", 
					normalizedEmail, userExists, customerExists);
				throw new com.example.fitnessgym_mg.exception.ConflictException(e.getMessage(), e);
			}
			log.error("Failed to create user in Supabase Auth: email={}, userExists={}, customerExists={}, supabaseUserExists={}, error={}", 
				normalizedEmail, userExists, customerExists, supabaseUserExists, e.getMessage(), e);
			throw new RuntimeException("Supabase Authでのユーザー作成に失敗しました: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error creating user in Supabase Auth: email={}, userExists={}, customerExists={}, supabaseUserExists={}, error={}", 
				normalizedEmail, userExists, customerExists, supabaseUserExists, e.getMessage(), e);
			throw new RuntimeException("Supabase Authでのユーザー作成に失敗しました: " + e.getMessage(), e);
		}

		// ユーザーの基本情報設定
		log.info("Creating local user record: email={}, authUserId={}", normalizedEmail, authUserId);
		User user = new User();
		setUserBasicFields(user, req);
		// 正規化されたメールアドレスを設定
		user.setEmail(normalizedEmail);
		// パスワードはハッシュ化してから設定（エンティティのsetPasswordはハッシュを受け取る）
		user.setPassword(passwordEncoder.encode(req.getPass()));
		// Supabase AuthのユーザーIDを設定
		user.setAuthUserId(authUserId);

		// 店舗の紐づけ
		Set<Store> storesToAssign = validateAndGetStores(storeIds);
		user.setStores(storesToAssign);
		userRepository.save(user);
		log.info("Successfully created user in local database: email={}, userId={}", normalizedEmail, user.getId());
	}

	// --- Admin用: ユーザー更新 ---
	@Transactional
	public void updateByAdmin(UUID id, UserRequest req, Set<UUID> storeIds) {
		// ユーザー取得（Admin用なので、storeIdチェック不要）
		User user = findUserOrThrow(id);

		// StoreIdsのnullチェック
		if (storeIds == null) {
			storeIds = Collections.emptySet();
		}

		// 店長ロールのバリデーション (単一の店舗必須)
		validateManagerRole(req.getRole(), storeIds);

		// 共通の更新ロジック
		validateAndUpdateUser(user, req, storeIds);
	}

	// --- Manager用: ユーザー更新 ---
	@Transactional
	public void updateByManager(UUID id, UserRequest req, UUID storeId) {
		// ユーザー取得
		User user = findUserOrThrow(id);
		// store所属チェック
		assertUserAssignedToStore(user, storeId);

		// Manager APIではstoreIdを強制追加
		Set<UUID> storeIds = new java.util.HashSet<>();
		storeIds.add(storeId);

		// 店長ロールのバリデーション (単一の店舗必須)
		validateManagerRole(req.getRole(), storeIds);

		// 共通の更新ロジック
		validateAndUpdateUser(user, req, storeIds);
	}

	// --- ユーザー更新の共通ロジック ---
	private void validateAndUpdateUser(User user, UserRequest req, Set<UUID> storeIds) {
		// 既存の更新ロジック
		setUserBasicFields(user, req);

		if (req.getPass() != null && !req.getPass().isEmpty()) {
			user.setPassword(passwordEncoder.encode(req.getPass()));
		}

		// 店舗の紐づけ情報の上書き
		Set<Store> storesToAssign = validateAndGetStores(storeIds);
		user.setStores(storesToAssign);
		userRepository.save(user);
	}

	// --- ユーザーを有効化 ---
	@Transactional
	public void enableActive(UUID id, UUID storeId) {
		User user = findUserOrThrow(id);
		assertUserAssignedToStore(user, storeId);
		
		if (user.isActive()) {
			return;
		}
		user.setActive(true);
		userRepository.save(user);
	}

	// --- ユーザーを無効化 ---
	@Transactional
	public void disableActive(UUID id, UUID storeId) {
		User user = findUserOrThrow(id);
		assertUserAssignedToStore(user, storeId);
		
		if (!user.isActive()) {
			return;
		}
		user.setActive(false);
		userRepository.save(user);
	}

	// 削除
	@Transactional
	public void delete(UUID id, UUID storeId) {
		User user = findUserOrThrow(id);
		assertUserAssignedToStore(user, storeId);

		if (user.isActive()) {
			throw new IllegalStateException("有効ユーザーは削除できません");
		}

		if (hasRelatedData(id)) {
			throw new IllegalStateException("このユーザーにはレッスン履歴が紐づいているため、削除できません。無効化してください。");
		}

		userRepository.delete(user);
	}

	// idでアカウント情報を取得
	@Transactional(readOnly = true)
	public UserResponse findById(UUID id, UUID storeId) {
		User user = findUserOrThrow(id);
		if (storeId != null) {
			assertUserAssignedToStore(user, storeId);
		}
		
		// stores関係を明示的にロード（LazyInitializationExceptionを防ぐ）
		// user_storesテーブルからstoresを取得して設定する
		String storesQuery = """
				SELECT us.user_id, s.id, s.name
				FROM user_stores us
				JOIN stores s ON us.store_id = s.id
				WHERE us.user_id = :userId
				""";
		
		@SuppressWarnings("unchecked")
		List<Object[]> storeResults = entityManager
				.createNativeQuery(storesQuery)
				.setParameter("userId", id)
				.getResultList();
		
		Set<Store> stores = new HashSet<>();
		for (Object[] row : storeResults) {
			try {
				UUID currentStoreId;
				if (row[1] instanceof UUID) {
					currentStoreId = (UUID) row[1];
				} else if (row[1] instanceof String) {
					currentStoreId = UUID.fromString((String) row[1]);
				} else {
					continue;
				}
				
				String storeName = row[2] != null ? row[2].toString() : null;
				if (storeName == null) {
					continue;
				}
				
				Store store = new Store();
				store.setId(currentStoreId);
				store.setName(storeName);
				stores.add(store);
			} catch (Exception e) {
				// マッピングエラーはスキップ
				continue;
			}
		}
		
		user.setStores(stores);
		
		return UserResponse.fromEntity(user);
	}

	// idでユーザーエンティティを取得（認可チェック用）
	@Transactional(readOnly = true)
	public User findUserEntityById(UUID id, UUID storeId) {
		User user = findUserOrThrow(id);
		if (storeId != null) {
			assertUserAssignedToStore(user, storeId);
		}
		return user;
	}

	// --- ヘルパーメソッド ---

	/**
	 * ユーザーの基本情報フィールドを設定（共通ロジック）
	 * 
	 * @param user ユーザーエンティティ
	 * @param req ユーザーリクエストDTO
	 */
	private void setUserBasicFields(User user, UserRequest req) {
		user.setEmail(req.getEmail());
		user.setName(req.getName());
		user.setKana(req.getKana());
		user.setRole(req.getRole());
		user.setActive(req.isActive());
	}

	/**
	 * ユーザーIDでユーザーを取得
	 * 
	 * <p>純粋にUserを取得するメソッド。store所属チェックは行わない。</p>
	 * 
	 * @param id ユーザーID
	 * @return ユーザーエンティティ
	 * @throws EntityNotFoundException ユーザーが見つからない場合
	 */
	private User findUserOrThrow(UUID id) {
		return userRepository.findById(id)
				.orElseThrow(() -> {
					return new com.example.fitnessgym_mg.exception.EntityNotFoundException("User not found with id: " + id);
				});
	}

	/**
	 * ユーザーが指定されたstoreに属しているかチェック
	 * 
	 * <p>ビジネスロジック: Manager APIでは、操作対象のユーザーが指定されたstoreに属している必要がある。</p>
	 * 
	 * @param user ユーザーエンティティ
	 * @param storeId 店舗ID
	 * @throws EntityNotFoundException ユーザーが指定されたstoreに属していない場合
	 */
	private void assertUserAssignedToStore(User user, UUID storeId) {
			if (user.getStores() == null || user.getStores().isEmpty()) {
			throw new com.example.fitnessgym_mg.exception.EntityNotFoundException("User not found with id: " + user.getId());
			}
			
			boolean isAssignedToStore = user.getStores().stream()
					.anyMatch(store -> store.getId().equals(storeId));

			if (!isAssignedToStore) {
			throw new com.example.fitnessgym_mg.exception.EntityNotFoundException("User not found with id: " + user.getId());
		}
	}

	private Sort createSort(UserSortType sort) {
		// 1. ロールによる昇順ソート（roleOrderプロパティは存在しないため、roleでソート）
		Sort primarySort = Sort.by("role").ascending();

		// 2. 登録日時降順ソート、またはカナ昇順ソート
		Sort secondarySort;
		if (sort == UserSortType.KANA) {
			secondarySort = Sort.by("kana").ascending();
		} else {
			// デフォルトおよびCREATEDの場合、登録日時降順
			secondarySort = Sort.by("createdAt").descending();
		}

		return primarySort.and(secondarySort);
	}

	private boolean hasRelatedData(UUID userId) {
		return lessonRepository.countByTrainerId(userId) > 0;
	}

	/**
	 * 店舗IDの検証と取得（共通ロジック）
	 * 
	 * @param storeIds 店舗IDのセット
	 * @return 検証済みの店舗エンティティのセット
	 */
	private Set<Store> validateAndGetStores(Set<UUID> storeIds) {
		if (storeIds == null || storeIds.isEmpty()) {
			return Collections.emptySet();
		}
		
		Set<Store> stores = storeRepository.findAllById(storeIds).stream()
				.collect(Collectors.toSet());
		
		if (stores.size() != storeIds.size()) {
			throw new com.example.fitnessgym_mg.exception.EntityNotFoundException("指定された店舗IDの一部が見つかりません。");
		}
		
		return stores;
	}

	/**
	 * 店長ロールのバリデーション（共通ロジック）
	 * 
	 * @param role ユーザーロール
	 * @param storeIds 店舗IDのセット
	 */
	private void validateManagerRole(UserRole role, Set<UUID> storeIds) {
		if (role == UserRole.MANAGER) {
			// 店長の場合、店舗IDが1つだけ存在することを確認
			if (storeIds == null || storeIds.size() != 1) {
				throw new com.example.fitnessgym_mg.exception.BusinessRuleViolationException("店長ユーザーには、割り当てる店舗を一つだけ選択する必要があります。");
			}
		}
	}

}