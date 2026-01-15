package com.example.fitnessgym_mg.service;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.JoinType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification; // ★ Specificationを追加 ★
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.config.ApplicationConstants;
import com.example.fitnessgym_mg.dto.request.CustomerRequest;
import com.example.fitnessgym_mg.dto.response.CustomerResponse;
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.CustomerSort;
import com.example.fitnessgym_mg.exception.InvalidRequestException;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.repository.CustomerRepositoryCustom;
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.repository.StoreRepository;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final LessonRepository lessonRepository;
	private final StoreRepository storeRepository;
	private final AuthorizationFacade authorizationFacade;
	private final SecurityUtil securityUtil;
	private final com.example.fitnessgym_mg.repository.UserRepository userRepository;

	// --- 顧客一覧検索（ページネーション対応） ---
	@Transactional(readOnly = true)
	public Page<CustomerResponse> searchCustomers(String keyword, CustomerSort sort, UUID storeId, Pageable pageable) {
		User currentUser = securityUtil.getCurrentUserOrThrow();

		// 認可チェック: 検索権限を検証
		authorizationFacade.checkCanSearchCustomers(currentUser, storeId);

		// 1. Specificationの構築
		// 論理削除条件はfindAllNotDeletedメソッドで自動的に適用されるため、明示的な合成は不要

		// distinct専用Specificationを分離
		Specification<Customer> distinctSpec = (root, query, cb) -> {
			query.distinct(true);
			return cb.conjunction();
		};

		Specification<Customer> spec = distinctSpec;

		// --- 1-1. 店舗IDによる絞り込み (中間テーブル Store_Customers 経由) ---
		if (storeId != null) {
			Specification<Customer> storeFilterSpec = (root, query, cb) -> cb
					.equal(root.join("stores", JoinType.INNER).get("id"), storeId);

			spec = spec.and(storeFilterSpec);
		}

		// --- 1-2. キーワードによる絞り込み (名前 OR かな) ---
		if (keyword != null && !keyword.isEmpty()) {
			String lowerKeyword = keyword.toLowerCase();
			spec = spec.and((root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("name")), "%" + lowerKeyword + "%"),
					cb.like(cb.lower(root.get("kana")), "%" + lowerKeyword + "%")));
		}

		// 2. ソートオブジェクトの生成
		CustomerSort safeSort = sort != null ? sort : CustomerSort.CREATED;
		Sort sortObj = switch (safeSort) {
		case KANA -> Sort.by("kana").ascending();
		case CREATED -> Sort.by("createdAt").descending();
		};

		// 3. PageableにSortを設定（pageSize制限を適用）
		int size = Math.min(pageable.getPageSize(), ApplicationConstants.MAX_PAGE_SIZE);
		Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), size, sortObj);

		// 4. 検索メソッドの実行 (findAllNotDeletedを使用 - 論理削除条件が自動適用される)
		Page<Customer> customerPage = customerRepository.findAllNotDeleted(spec, sortedPageable);

		// 5. マッピング
		return customerPage.map(CustomerResponse::fromEntity);
	}

	// --- 顧客作成 ---
	public void create(CustomerRequest req, UUID storeIdFromPath) {
		User currentUser = securityUtil.getCurrentUserOrThrow();

		// storeIdの決定: パス変数があればそれを優先、なければリクエストボディから取得
		// 注意: 現在の実装では、ADMINとMANAGERの両方でリクエストボディのstoreIdを使用するため、
		// storeIdFromPathは後方互換性のために残しているが、通常はnullになる
		UUID storeId = storeIdFromPath != null ? storeIdFromPath : req.getStoreId();

		// バリデーション: 作成時のビジネスルールチェック
		validateForCreate(req);

		Customer customer = new Customer();

		// 必須項目を設定
		setCustomerBasicFields(customer, req);

		// 任意項目を設定
		customer.setMedical(req.getMedical());
		customer.setTaboo(req.getTaboo());
		customer.setMemo(req.getMemo());

		// first_posture_group_id は新規作成時は任意 (null許容)
		customer.setFirstPostureGroupId(req.getFirstPostureGroupId());

		// システム設定
		customer.setActive(true); // is_active は新規作成時は有効 (true)

		// Storeとの紐付け（storeIdが指定されている場合のみ）
		// ADMINとMANAGERの両方: リクエストボディのstoreIdを使用して店舗に紐付ける（顧客がどの店舗で登録されたかを記録するため）
		// storeIdFromPathは後方互換性のために残しているが、通常はnullになる
		if (storeId != null) {
			// 認可チェック: 店舗へのアクセス権を検証
			authorizationFacade.checkCanAccessStoreOrThrow(currentUser, storeId);

			// Storeエンティティの取得
			Store store = storeRepository.findById(storeId)
					.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException(
							"店舗が見つかりません: " + storeId));

			// Storeとの紐付け（store_customersテーブルに保存される）
			customer.addStore(store);
		}

		customerRepository.save(customer);
	}

	// --- 顧客更新 ---
	public void update(UUID id, CustomerRequest req) {
		User currentUser = securityUtil.getCurrentUserOrThrow();
		Customer customer = getAuthorizedCustomer(id, currentUser);

		// email変更時の重複チェック
		if (!customer.getEmail().equals(req.getEmail())) {
			// emailが変更されている場合のみチェック
			// @SQLRestrictionを回避するため、ネイティブSQLクエリを使用するexistsByEmailAndIdNotNative()を使用
			// CustomerRepositoryをCustomerRepositoryCustomにキャストして直接呼び出す
			if (customerRepository instanceof CustomerRepositoryCustom) {
				boolean emailExists = ((CustomerRepositoryCustom) customerRepository)
						.existsByEmailAndIdNotNative(req.getEmail(), id);
				if (emailExists) {
					throw new com.example.fitnessgym_mg.exception.InvalidRequestException("このメールアドレスは既に登録されています");
				}
			} else {
				throw new RuntimeException("CustomerRepository does not implement CustomerRepositoryCustom");
			}
		}

		// 必須項目を更新
		setCustomerBasicFields(customer, req);

		// 任意項目を更新
		customer.setMedical(req.getMedical());
		customer.setTaboo(req.getTaboo());
		customer.setMemo(req.getMemo());

		// システム設定
		customer.setActive(req.isActive()); // 有効/無効状態も更新できる想定

		// バリデーション: 更新時のビジネスルールチェック
		validateForUpdate(req);
		
		// firstPostureGroupIdの更新（nullの場合は既存の値を保持）
		if (req.getFirstPostureGroupId() != null) {
			customer.setFirstPostureGroupId(req.getFirstPostureGroupId());
		}
		// req.getFirstPostureGroupId()がnullの場合は、既存のfirstPostureGroupIdを保持（変更しない）

		customerRepository.save(customer);
	}

	// --- 有効/無効切替 ---
	public void enableActive(UUID id) {
		User currentUser = securityUtil.getCurrentUserOrThrow();
		Customer customer = getAuthorizedCustomer(id, currentUser);
		customer.setActive(true);
		customerRepository.save(customer);
	}

	public void disableActive(UUID id) {
		User currentUser = securityUtil.getCurrentUserOrThrow();
		Customer customer = getAuthorizedCustomer(id, currentUser);
		customer.setActive(false);
		customerRepository.save(customer);
	}

	// --- 削除 ---
	public void delete(UUID id) {
		User currentUser = securityUtil.getCurrentUserOrThrow();
		Customer customer = getAuthorizedCustomer(id, currentUser);
		
		// デバッグ: 顧客の状態をログに出力
		log.debug("顧客削除リクエスト: customerId={}, active={}, deletedAt={}", 
			id, customer.isActive(), customer.getDeletedAt());

		// ドメイン制約の検証
		customer.validateDeletable();

		// 論理削除を実行（物理削除は行わない）
		// 注意: hasRelatedDataチェックは削除（論理削除のため、レッスンデータは統計に表示される）
		customer.setDeletedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
		customerRepository.save(customer);
		log.info("顧客削除成功: customerId={}", id);
	}

	// IDでエンティティを取得する（編集モーダル初期表示用など）
	@Transactional(readOnly = true)
	public Customer findById(UUID id) {
		User currentUser = securityUtil.getCurrentUserOrThrow();
		return getAuthorizedCustomer(id, currentUser);
	}

	/**
	 * 認可チェック済みの顧客エンティティを取得
	 * 
	 * <p>このメソッドは認可チェック、論理削除チェック、エンティティ取得を一元化する。</p>
	 * <p>すべてのpublicメソッドでこのメソッドを使用することで、認可漏れと論理削除済み顧客の操作を防止する。</p>
	 * <p>Repositoryへの直接アクセスはこのメソッド内でのみ許可され、構造的に誤用を防止する。</p>
	 * 
	 * @param id 顧客ID
	 * @param currentUser 現在のユーザー
	 * @return 顧客エンティティ
	 * @throws AccessDeniedException 認可不可の場合
	 * @throws EntityNotFoundException 顧客が見つからない、または論理削除されている場合
	 */
	@Transactional(readOnly = true)
	private Customer getAuthorizedCustomer(UUID id, User currentUser) {
		// 1. エンティティ取得（Repository直接アクセス）
		// @SQLRestrictionを回避するために、ネイティブSQLクエリを使用するfindByIdWithStoresNative()を使用
		// CustomerRepositoryをCustomerRepositoryCustomにキャストして直接呼び出す
		Customer customer;
		if (customerRepository instanceof CustomerRepositoryCustom) {
			customer = ((CustomerRepositoryCustom) customerRepository)
					.findByIdWithStoresNative(id)
					.orElseThrow(() -> {
						log.warn("顧客が見つかりません: customerId={}", id);
						return new com.example.fitnessgym_mg.exception.EntityNotFoundException("顧客が見つかりません: " + id);
					});
		} else {
			throw new RuntimeException("CustomerRepository does not implement CustomerRepositoryCustom");
		}

		// 2. 状態検証: 論理削除チェック
		if (customer.isDeleted()) {
			log.warn("論理削除済みの顧客にアクセスしようとしました: customerId={}", id);
			throw new com.example.fitnessgym_mg.exception.EntityNotFoundException("顧客が見つかりません: " + id);
		}

		// 3. 認可チェック（エンティティ版を使用）
		authorizationFacade.checkCanAccessCustomerOrThrow(currentUser, customer);

		return customer;
	}

	// --- トレーナー用顧客取得 ---
	/**
	 * 現在ログイン中のトレーナーが所属店舗の全ての顧客リストを取得
	 * トレーナーが所属する店舗の顧客のみを取得
	 */
	@Transactional(readOnly = true)
	public List<CustomerResponse> getAllCustomersForTrainerStores() {
		User currentUser = securityUtil.getCurrentUserOrThrow();
		UUID trainerId = currentUser.getId();
		
		log.info("トレーナーが顧客を取得: trainerId={}", trainerId);
		
		// トレーナーにはuser_storesテーブルにレコードがないため、店舗フィルタリングを行わず全顧客を取得
		// 店舗と顧客の紐付けは維持されるが、トレーナーは全顧客を閲覧可能
		
		// デバッグ: 全顧客数を確認（店舗フィルタリング前）
		Specification<Customer> allCustomersSpec = (root, query, cb) -> cb.conjunction();
		Page<Customer> allCustomersPage = customerRepository.findAllNotDeleted(allCustomersSpec, Pageable.unpaged());
		long totalCustomersCount = allCustomersPage.getTotalElements();
		log.info("全顧客数（論理削除されていない）: count={}", totalCustomersCount);
		
		// 店舗フィルタリングなしで、論理削除されていない全顧客を取得
		Specification<Customer> distinctSpec = (root, query, cb) -> {
			query.distinct(true);
			return cb.conjunction();
		};
		
		// 論理削除されていない、かつ有効な顧客のみを取得
		// Pageable.unpaged()を使用して全件取得
		Page<Customer> customerPage = customerRepository.findAllNotDeleted(distinctSpec, Pageable.unpaged());
		List<Customer> customers = customerPage.getContent();
		
		log.info("トレーナーが取得した顧客数: trainerId={}, count={}", trainerId, customers.size());
		
		// デバッグ: 取得された顧客のIDと名前をログ出力
		if (log.isDebugEnabled() || customers.isEmpty()) {
			if (customers.isEmpty()) {
				log.warn("顧客が取得できませんでした: trainerId={}, totalCustomers={}", 
					trainerId, totalCustomersCount);
			} else {
				customers.forEach(customer -> 
					log.debug("取得された顧客: customerId={}, name={}, active={}, stores={}", 
						customer.getId(), customer.getName(), customer.isActive(),
						customer.getStores() != null ? customer.getStores().stream()
							.map(s -> s.getId() + "(" + s.getName() + ")")
							.collect(Collectors.joining(", ")) : "null")
				);
			}
		}
		
		return customers.stream()
				.filter(Customer::isActive)
				.map(CustomerResponse::fromEntity)
				.collect(Collectors.toList());
	}

	// --- 顧客IDで顧客詳細を取得（CustomerResponse形式） ---
	/**
	 * 顧客IDで顧客情報を取得し、CustomerResponseに変換
	 * 最新レッスンの体重も取得して設定
	 */
	@Transactional(readOnly = true)
	public CustomerResponse getCustomerById(UUID customerId) {
		User currentUser = securityUtil.getCurrentUserOrThrow();
		Customer customer = getAuthorizedCustomer(customerId, currentUser);
		CustomerResponse response = CustomerResponse.fromEntity(customer);

		// 最新レッスンの体重を取得（パフォーマンス最適化：1件のみ取得）
		// 注意: CustomerエンティティにLessonへの直接リレーションがないため、別クエリが必要
		org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
				com.example.fitnessgym_mg.config.ApplicationConstants.DEFAULT_PAGE_NUMBER, 1);
		java.util.List<com.example.fitnessgym_mg.entity.Lesson> lessons = lessonRepository
				.findLatestLessonsWithWeightByCustomerId(customerId, pageable);
		if (!lessons.isEmpty()) {
			response.setLatestWeight(lessons.get(0).getWeight());
		}

		return response;
	}

	// --- ヘルパーメソッド ---

	/**
	 * 顧客作成時のバリデーション
	 * 
	 * <p>新規作成時はfirstPostureGroupIdは任意（null許容）。</p>
	 * <p>初回姿勢画像は後から登録可能なため。</p>
	 * 
	 * @param req 顧客リクエストDTO
	 * @throws InvalidRequestException バリデーションエラーの場合
	 */
	private void validateForCreate(CustomerRequest req) {
		// emailの重複チェック
		// @SQLRestrictionを回避するため、ネイティブSQLクエリを使用するexistsByEmailNative()を使用
		// CustomerRepositoryをCustomerRepositoryCustomにキャストして直接呼び出す
		if (customerRepository instanceof CustomerRepositoryCustom) {
			boolean emailExists = ((CustomerRepositoryCustom) customerRepository)
					.existsByEmailNative(req.getEmail());
			if (emailExists) {
				throw new com.example.fitnessgym_mg.exception.InvalidRequestException("このメールアドレスは既に登録されています");
			}
		} else {
			throw new RuntimeException("CustomerRepository does not implement CustomerRepositoryCustom");
		}
	}

	/**
	 * 顧客更新時のバリデーション
	 * 
	 * <p>更新時はfirstPostureGroupIdは任意（null許容）。</p>
	 * <p>初回姿勢画像は後から登録可能なため。</p>
	 * 
	 * @param req 顧客リクエストDTO
	 * @throws InvalidRequestException バリデーションエラーの場合
	 */
	private void validateForUpdate(CustomerRequest req) {
		// firstPostureGroupIdは任意のため、バリデーションは不要
		// 必要に応じて他のバリデーションを追加可能
	}

	/**
	 * 顧客の基本情報フィールドを設定（共通ロジック）
	 * 
	 * @param customer 顧客エンティティ
	 * @param req 顧客リクエストDTO
	 */
	private void setCustomerBasicFields(Customer customer, CustomerRequest req) {
		customer.setKana(req.getKana());
		customer.setName(req.getName());
		customer.setGender(req.getGender());
		customer.setBirthday(req.getBirthday());
		customer.setHeight(req.getHeight());
		customer.setEmail(req.getEmail());
		customer.setPhone(req.getPhone());
		customer.setAddress(req.getAddress());
	}

}