package com.example.fitnessgym_mg.service;

import java.time.ZoneOffset;
import java.util.List;
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
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.repository.StoreRepository;
import com.example.fitnessgym_mg.repository.UserCustomerRepository;
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
	private final UserCustomerRepository userCustomerRepository;
	private final StoreRepository storeRepository;
	private final AuthorizationFacade authorizationFacade;
	private final SecurityUtil securityUtil;

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
		UUID storeId = storeIdFromPath != null ? storeIdFromPath : req.getStoreId();

		if (storeId == null) {
			throw new com.example.fitnessgym_mg.exception.InvalidRequestException("店舗IDは必須です");
		}

		// 認可チェック: 店舗へのアクセス権を検証
		authorizationFacade.checkCanAccessStoreOrThrow(currentUser, storeId);

		// Storeエンティティの取得
		Store store = storeRepository.findById(storeId)
				.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException(
						"店舗が見つかりません: " + storeId));

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

		// Storeとの紐付け
		customer.addStore(store);

		customerRepository.save(customer);
	}

	// --- 顧客更新 ---
	public void update(UUID id, CustomerRequest req) {
		User currentUser = securityUtil.getCurrentUserOrThrow();
		Customer customer = getAuthorizedCustomer(id, currentUser);

		// email変更時の重複チェック
		if (!customer.getEmail().equals(req.getEmail())) {
			// emailが変更されている場合のみチェック
			if (customerRepository.existsByEmailAndIdNot(req.getEmail(), id)) {
				throw new com.example.fitnessgym_mg.exception.InvalidRequestException("このメールアドレスは既に登録されています");
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
		customer.setFirstPostureGroupId(req.getFirstPostureGroupId());

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

		// ドメイン制約の検証
		customer.validateDeletable();

		// 論理削除を実行（物理削除は行わない）
		// 注意: hasRelatedDataチェックは削除（論理削除のため、レッスンデータは統計に表示される）
		customer.setDeletedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC));
		customerRepository.save(customer);
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
		Customer customer = customerRepository.findByIdWithStores(id)
				.orElseThrow(() -> {
					log.warn("顧客が見つかりません: customerId={}", id);
					return new com.example.fitnessgym_mg.exception.EntityNotFoundException("顧客が見つかりません: " + id);
				});

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
	 * 現在ログイン中のトレーナーが担当している顧客リストを取得
	 * UserCustomerRepositoryを使用して中間テーブル経由で取得し、CustomerResponseに変換
	 */
	@Transactional(readOnly = true)
	public List<CustomerResponse> getMyCustomers() {
		User currentUser = securityUtil.getCurrentUserOrThrow();
		UUID trainerId = currentUser.getId();

		return userCustomerRepository.findByUserIdWithCustomer(trainerId).stream()
				.filter(uc -> !uc.getCustomer().isDeleted() && uc.getCustomer().isActive())
				.map(uc -> CustomerResponse.fromEntity(uc.getCustomer()))
				.collect(Collectors.toList());
	}

	/**
	 * トレーナーが顧客に割り当てられているか確認（存在確認専用）
	 * 
	 * @param trainerId トレーナーID
	 * @param customerId 顧客ID
	 * @return 割り当てられている場合 true
	 */
	@Transactional(readOnly = true)
	public boolean isTrainerAssignedToCustomer(UUID trainerId, UUID customerId) {
		return userCustomerRepository.existsById(
				new com.example.fitnessgym_mg.entity.UserCustomer.UserCustomerId(trainerId, customerId));
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
		if (customerRepository.existsByEmail(req.getEmail())) {
			throw new com.example.fitnessgym_mg.exception.InvalidRequestException("このメールアドレスは既に登録されています");
		}
	}

	/**
	 * 顧客更新時のバリデーション
	 * 
	 * <p>更新時はfirstPostureGroupIdは必須。</p>
	 * <p>既存顧客には必ず初回姿勢画像が登録されている必要があるため。</p>
	 * 
	 * @param req 顧客リクエストDTO
	 * @throws InvalidRequestException バリデーションエラーの場合
	 */
	private void validateForUpdate(CustomerRequest req) {
		if (req.getFirstPostureGroupId() == null) {
			throw new com.example.fitnessgym_mg.exception.InvalidRequestException("初回姿勢画像は必須です");
		}
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