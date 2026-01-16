package com.example.fitnessgym_mg.controller.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fitnessgym_mg.dto.request.CustomerRequest;
import com.example.fitnessgym_mg.dto.response.CustomerResponse;
import com.example.fitnessgym_mg.entity.enums.CustomerSort;
import com.example.fitnessgym_mg.service.CustomerService;
import com.example.fitnessgym_mg.util.EmailHashUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 顧客管理REST APIコントローラー
 * すべてのエンドポイントはJSONを返します
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerApiController {

	private final CustomerService service;

	// ========== REST API エンドポイント ==========

	/**
	 * GET /api/customers
	 * 顧客一覧取得（オプション選択用）
	 * 認証済みユーザー全員がアクセス可能
	 * ページングなしで全顧客を返す
	 * 
	 * <p>注意: @SQLRestrictionを回避するために、ネイティブSQLクエリを使用して
	 * idとnameのみを取得し、直接CustomerResponseを作成します。</p>
	 */
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TRAINER')")
	@GetMapping("/customers")
	public ResponseEntity<List<CustomerResponse>> getCustomersForOptions() {
		log.debug("顧客一覧取得（オプション選択用）リクエスト");
		
		List<CustomerResponse> customers = service.getAllCustomersForOptions();
		
		log.info("顧客一覧取得（オプション選択用）成功: count={}", customers.size());
		return ResponseEntity.ok(customers);
	}

	/**
	 * GET /api/admin/customers
	 * 顧客一覧取得
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/admin/customers")
	public ResponseEntity<Page<CustomerResponse>> getAdminCustomers(
			@RequestParam(required = false) @jakarta.validation.constraints.Size(max = 100, message = "Keyword must be less than 100 characters") String name,
			@RequestParam(defaultValue = "created") String sort,
			@RequestParam(required = false) UUID storeId,
			@RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(value = 0, message = "Page must be 0 or greater") int page,
			@RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(value = 1, message = "Size must be at least 1") @jakarta.validation.constraints.Max(value = 100, message = "Size must not exceed 100") int size) {

		Pageable pageable = PageRequest.of(page, size);
		CustomerSort sortEnum = CustomerSort.fromString(sort);
		Page<CustomerResponse> customerPage = service.searchCustomers(name, sortEnum, storeId, pageable);
		return ResponseEntity.ok(customerPage);
	}

	/**
	 * GET /api/admin/customers?name={keyword}
	 * 顧客名による検索（上記のgetAdminCustomersメソッドでnameパラメータとして処理）
	 */

	/**
	 * POST /api/admin/customers
	 * 顧客の新規登録
	 * 
	 * <p>ADMINが顧客を作成する場合、リクエストボディのstoreIdを使用して店舗に紐付けます。</p>
	 * <p>storeIdが指定されている場合、その店舗に紐付けてstore_customersテーブルに保存します。</p>
	 * <p>storeIdがnullの場合、店舗に紐付けずに作成します。</p>
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/admin/customers")
	public ResponseEntity<Void> createAdminCustomer(@Valid @RequestBody CustomerRequest request) {
		log.debug("顧客作成リクエスト受信: name={}, emailHash={}, storeId={}", request.getName(), EmailHashUtil.hashEmail(request.getEmail()), request.getStoreId());
		// ADMINの場合、リクエストボディのstoreIdを使用（nullの場合は店舗に紐付けない）
		service.create(request, request.getStoreId());
		log.info("顧客作成成功: name={}, emailHash={}, storeId={}", request.getName(), EmailHashUtil.hashEmail(request.getEmail()), request.getStoreId());
		return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).build();
	}

	/**
	 * PATCH /api/admin/customers/{customer_id}/disable
	 * 顧客の無効化
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/admin/customers/{customer_id}/disable")
	public ResponseEntity<Void> disableAdminCustomer(@PathVariable("customer_id") UUID customerId) {
		service.disableActive(customerId);
		return ResponseEntity.ok().build();
	}

	/**
	 * PATCH /api/admin/customers/{customer_id}/enable
	 * 顧客の再有効化
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/admin/customers/{customer_id}/enable")
	public ResponseEntity<Void> enableAdminCustomer(@PathVariable("customer_id") UUID customerId) {
		service.enableActive(customerId);
		return ResponseEntity.ok().build();
	}

	/**
	 * DELETE /api/admin/customers/{customer_id}
	 * 顧客の削除
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/admin/customers/{customer_id}")
	public ResponseEntity<Void> deleteAdminCustomer(@PathVariable("customer_id") UUID customerId) {
		service.delete(customerId);
		return ResponseEntity.ok().build();
	}

	/**
	 * GET /api/stores/{store_id}/manager/customers
	 * 顧客一覧取得(店舗内管轄)
	 */
	@PreAuthorize("hasRole('MANAGER') and @authorizationFacade.canAccessStore(authentication, #storeId)")
	@GetMapping("/stores/{store_id}/manager/customers")
	public ResponseEntity<Page<CustomerResponse>> getManagerCustomers(
			@PathVariable("store_id") UUID storeId,
			@RequestParam(required = false) @jakarta.validation.constraints.Size(max = 100, message = "Keyword must be less than 100 characters") String name,
			@RequestParam(defaultValue = "created") String sort,
			@RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(value = 0, message = "Page must be 0 or greater") int page,
			@RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(value = 1, message = "Size must be at least 1") @jakarta.validation.constraints.Max(value = 100, message = "Size must not exceed 100") int size) {

		Pageable pageable = PageRequest.of(page, size);
		CustomerSort sortEnum = CustomerSort.fromString(sort);
		Page<CustomerResponse> customerPage = service.searchCustomers(name, sortEnum, storeId, pageable);
		return ResponseEntity.ok(customerPage);
	}

	/**
	 * GET /api/stores/{store_id}/manager/customers?name={keyword}
	 * 顧客名による検索(店舗内管轄)（上記のgetManagerCustomersメソッドでnameパラメータとして処理）
	 */

	/**
	 * POST /api/manager/customers
	 * 顧客の新規登録(店舗内管轄)
	 * 
	 * <p>MANAGERが顧客を作成する場合、リクエストボディのstoreIdを使用して店舗に紐付けます。</p>
	 * <p>storeIdが指定されている場合、その店舗に紐付けてstore_customersテーブルに保存します。</p>
	 */
	@PreAuthorize("hasRole('MANAGER')")
	@PostMapping("/manager/customers")
	public ResponseEntity<Void> createManagerCustomer(
			@Valid @RequestBody CustomerRequest request) {
		log.debug("顧客作成リクエスト受信: name={}, emailHash={}, storeId={}", request.getName(), EmailHashUtil.hashEmail(request.getEmail()), request.getStoreId());
		// MANAGERの場合、リクエストボディのstoreIdを使用（nullの場合は店舗に紐付けない）
		// 認可チェックはCustomerService内で実施
		service.create(request, request.getStoreId());
		log.info("顧客作成成功: name={}, emailHash={}, storeId={}", request.getName(), EmailHashUtil.hashEmail(request.getEmail()), request.getStoreId());
		return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).build();
	}

	/**
	 * PATCH /api/stores/{store_id}/manager/customers/{customer_id}/disable
	 * 顧客の無効化(店舗内管轄)
	 */
	@PreAuthorize("hasRole('MANAGER') and @authorizationFacade.canAccessStore(authentication, #storeId)")
	@PatchMapping("/stores/{store_id}/manager/customers/{customer_id}/disable")
	public ResponseEntity<Void> disableManagerCustomer(
			@PathVariable("store_id") UUID storeId,
			@PathVariable("customer_id") UUID customerId) {
		service.disableActive(customerId);
		return ResponseEntity.ok().build();
	}

	/**
	 * PATCH /api/stores/{store_id}/manager/customers/{customer_id}/enable
	 * 顧客の再有効化(店舗内管轄)
	 */
	@PreAuthorize("hasRole('MANAGER') and @authorizationFacade.canAccessStore(authentication, #storeId)")
	@PatchMapping("/stores/{store_id}/manager/customers/{customer_id}/enable")
	public ResponseEntity<Void> enableManagerCustomer(
			@PathVariable("store_id") UUID storeId,
			@PathVariable("customer_id") UUID customerId) {
		service.enableActive(customerId);
		return ResponseEntity.ok().build();
	}

	/**
	 * DELETE /api/stores/{store_id}/manager/customers/{customer_id}
	 * 顧客の削除(店舗内管轄)
	 */
	@PreAuthorize("hasRole('MANAGER') and @authorizationFacade.canAccessStore(authentication, #storeId)")
	@DeleteMapping("/stores/{store_id}/manager/customers/{customer_id}")
	public ResponseEntity<Void> deleteManagerCustomer(
			@PathVariable("store_id") UUID storeId,
			@PathVariable("customer_id") UUID customerId) {
		service.delete(customerId);
		return ResponseEntity.ok().build();
	}

	/**
	 * GET /api/trainers/customers
	 * 担当顧客の一覧取得（storeId不要版）
	 * 現在ログイン中のトレーナーが所属する店舗の全ての顧客リストを取得
	 */
	@PreAuthorize("hasRole('TRAINER')")
	@GetMapping("/trainers/customers")
	public ResponseEntity<java.util.List<CustomerResponse>> getTrainerCustomersWithoutStoreId() {
		java.util.List<CustomerResponse> customers = service.getAllCustomersForTrainerStores();
		return ResponseEntity.ok(customers);
	}

	/**
	 * GET /api/customers/{customer_id}/profile
	 * 顧客の基本プロフィール情報取得
	 */
	@PreAuthorize("@authorizationFacade.canAccessCustomer(authentication, #customerId)")
	@GetMapping("/customers/{customer_id}/profile")
	public ResponseEntity<CustomerResponse> getCustomerProfile(@PathVariable("customer_id") UUID customerId) {
		CustomerResponse customer = service.getCustomerById(customerId);
		return ResponseEntity.ok(customer);
	}

	/**
	 * PATCH /api/customers/{customer_id}/profile
	 * 顧客プロフィールの更新
	 */
	@PreAuthorize("@authorizationFacade.canAccessCustomer(authentication, #customerId)")
	@PatchMapping("/customers/{customer_id}/profile")
	public ResponseEntity<Void> updateCustomerProfile(
			@PathVariable("customer_id") UUID customerId,
			@Valid @RequestBody CustomerRequest request) {
		service.update(customerId, request);
		return ResponseEntity.ok().build();
	}
}