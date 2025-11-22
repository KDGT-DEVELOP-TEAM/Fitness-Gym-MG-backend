package com.example.fitnessgym_mg.service;

import java.util.UUID;

import jakarta.persistence.criteria.JoinType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification; // ★ Specificationを追加 ★
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.request.CustomerRequest;
import com.example.fitnessgym_mg.dto.response.CustomerResponse;
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.repository.LessonRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final LessonRepository lessonRepository;

	// --- 顧客一覧検索（ページネーション対応） ---
	@Transactional(readOnly = true)
	public Page<CustomerResponse> searchCustomers(String keyword, String sort, UUID storeId, Pageable pageable) {

		// 1. Specificationの構築
		Specification<Customer> spec = (root, query, cb) -> null;

		// --- 1-1. 店舗IDによる絞り込み (中間テーブル Store_Customers 経由) ---
		if (storeId != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.join("stores", JoinType.INNER).get("id"), storeId));
		}

		// --- 1-2. キーワードによる絞り込み (名前 OR かな) ---
		if (keyword != null && !keyword.isEmpty()) {
			String lowerKeyword = keyword.toLowerCase();
			spec = spec.and((root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("name")), "%" + lowerKeyword + "%"),
					cb.like(cb.lower(root.get("kana")), "%" + lowerKeyword + "%")));
		}

		// 2. ソートオブジェクトの生成
		Sort sortObj = switch (sort) {
		case "kana" -> Sort.by("kana").ascending();
		case "created" -> Sort.by("createdAt").descending();
		default -> Sort.by("createdAt").descending();
		};

		// 3. PageableにSortを設定
		Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortObj);

		// 4. 検索メソッドの実行 (findAll(Specification, Pageable) を使用)
		Page<Customer> customerPage = customerRepository.findAll(spec, sortedPageable);

		// 5. マッピング
		return customerPage.map(CustomerResponse::fromEntity);
	}

	// --- 顧客作成（店長操作の場合、storeIdを意識する） ---
	public void create(CustomerRequest req, UUID storeId) {
		Customer customer = new Customer();

		// 必須項目を設定
		customer.setKana(req.getKana());
		customer.setName(req.getName());
		customer.setGender(req.getGender());
		customer.setBirthday(req.getBirthday());
		customer.setHeight(req.getHeight());
		customer.setEmail(req.getEmail());
		customer.setPhone(req.getPhone());
		customer.setAddress(req.getAddress());

		// 任意項目を設定
		customer.setMedical(req.getMedical());
		customer.setTaboo(req.getTaboo());
		customer.setMemo(req.getMemo());

		// first_posture_group_id は新規作成時は任意 (null許容)
		customer.setFirstPostureGroupId(req.getFirstPostureGroupId());

		// システム設定
		customer.setActive(true); // is_active は新規作成時は有効 (true)

		// NOTE: 多対多の場合、Customer保存後に Store_Customers テーブルへの登録処理が必要です。
		// ここでは、Customerエンティティのみ保存。中間テーブル登録は呼び出し元に委ねるか、
		// 別のService層で実行すると想定します。

		customerRepository.save(customer);
	}

	// --- 顧客更新（権限チェックを追加） ---
	public void update(UUID id, CustomerRequest req, UUID storeId) {
		Customer customer = findCustomerById(id, storeId); // 権限チェック付き取得

		// 必須項目を更新
		customer.setKana(req.getKana());
		customer.setName(req.getName());
		customer.setGender(req.getGender());
		customer.setBirthday(req.getBirthday());
		customer.setHeight(req.getHeight());
		customer.setEmail(req.getEmail());
		customer.setPhone(req.getPhone());
		customer.setAddress(req.getAddress());

		// 任意項目を更新
		customer.setMedical(req.getMedical());
		customer.setTaboo(req.getTaboo());
		customer.setMemo(req.getMemo());

		// システム設定
		customer.setActive(req.isActive()); // 有効/無効状態も更新できる想定

		// 編集時の初回姿勢画像必須チェック（ビジネスロジック）
		if (req.getFirstPostureGroupId() == null) {
			throw new IllegalArgumentException("初回姿勢画像 (firstPostureGroupId) は編集時に必須です。");
		}
		customer.setFirstPostureGroupId(req.getFirstPostureGroupId());

		customerRepository.save(customer);
	}

	// --- 有効/無効切替 ---
	public void enableActive(UUID id, UUID storeId) {
		Customer customer = findCustomerById(id, storeId);
		customer.setActive(true);
		customerRepository.save(customer);
	}

	public void disableActive(UUID id, UUID storeId) {
		Customer customer = findCustomerById(id, storeId);
		customer.setActive(false);
		customerRepository.save(customer);
	}

	// --- 削除（権限チェックを追加） ---
	public void delete(UUID id, UUID storeId) { // ★ storeId を追加 ★
		Customer customer = findCustomerById(id, storeId); // 権限チェック付き取得

		// 有効ユーザーは削除不可
		if (customer.isActive()) {
			throw new IllegalStateException("有効ユーザーは削除できません");
		}

		if (hasRelatedData(id)) {
			// 関連データが存在する場合、例外をスローして削除を拒否
			throw new IllegalStateException("この顧客にはレッスン履歴などの関連データが存在するため、削除できません。無効化してください。");
		}

		customerRepository.delete(customer);
	}

	// IDでエンティティを取得する（編集モーダル初期表示用など、権限チェックを追加）
	@Transactional(readOnly = true)
	public Customer findById(UUID id, UUID storeId) { // ★ storeId を追加 ★
		return findCustomerById(id, storeId);
	}

	// --- ヘルパーメソッド ---

	/**
	 * IDでCustomerエンティティを取得し、storeIdが提供されていれば中間テーブル経由の権限チェックを行う
	 */
	private Customer findCustomerById(UUID id, UUID storeId) {
		Customer customer = customerRepository.findById(id)
				// RuntimeExceptionに置き換え
				.orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

		// 店長の場合 (storeId != null)、操作対象の顧客が自分の店舗に属するかチェック
		if (storeId != null) {
			// Customerが持つ stores コレクションに、該当 storeId が存在するか確認
			boolean isAssignedToStore = customer.getStores().stream()
					.anyMatch(store -> store.getId().equals(storeId));

			if (!isAssignedToStore) {
				// 権限外の顧客への操作は拒否
				throw new RuntimeException("Customer not found (or access denied) with id: " + id);
			}
		}
		return customer;
	}

	// --- ヘルパーメソッド: 関連データの存在チェック ---
	/**
	 * 指定された顧客IDに関連するデータ（例: レッスンなど）が存在するか確認する
	 */
	private boolean hasRelatedData(UUID customerId) {
		// 例: レッスンリポジトリで顧客IDに紐づくレコードが1件でもあるかチェック
		return lessonRepository.countByCustomerId(customerId) > 0;
	}
}