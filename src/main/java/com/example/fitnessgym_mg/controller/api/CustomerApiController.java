package com.example.fitnessgym_mg.controller.api;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import com.example.fitnessgym_mg.exception.EntityNotFoundException;
import com.example.fitnessgym_mg.service.CustomerAuthorizationService;
import com.example.fitnessgym_mg.service.CustomerService;
import com.example.fitnessgym_mg.util.SecurityUtil;

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
	private final SecurityUtil securityUtil;
	private final CustomerAuthorizationService customerAuthorizationService;

	// ========== REST API エンドポイント ==========

	/**
	 * GET /api/admin/customers
	 * 顧客一覧取得
	 */
	@GetMapping("/admin/customers")
	public ResponseEntity<Page<CustomerResponse>> getAdminCustomers(
			@RequestParam(required = false) @jakarta.validation.constraints.Size(max = 100, message = "Keyword must be less than 100 characters") String name,
			@RequestParam(defaultValue = "created") String sort,
			@RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(value = 0, message = "Page must be 0 or greater") int page,
			@RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(value = 1, message = "Size must be at least 1") @jakarta.validation.constraints.Max(value = 100, message = "Size must not exceed 100") int size) {

		Pageable pageable = PageRequest.of(page, size);
		Page<CustomerResponse> customerPage = service.searchCustomers(name, sort, null, pageable);
		return ResponseEntity.ok(customerPage);
	}

	/**
	 * GET /api/admin/customers?name={keyword}
	 * 顧客名による検索（上記のgetAdminCustomersメソッドでnameパラメータとして処理）
	 */

	/**
	 * POST /api/admin/customers
	 * 顧客の新規登録
	 */
	@PostMapping("/admin/customers")
	public ResponseEntity<Void> createAdminCustomer(@Valid @RequestBody CustomerRequest request) {
		service.create(request);
		return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).build();
	}

	/**
	 * PATCH /api/admin/customers/{customer_id}/disable
	 * 顧客の無効化
	 */
	@PatchMapping("/admin/customers/{customer_id}/disable")
	public ResponseEntity<Void> disableAdminCustomer(@PathVariable("customer_id") UUID customerId) {
		service.disableActive(customerId, null);
		return ResponseEntity.ok().build();
	}

	/**
	 * PATCH /api/admin/customers/{customer_id}/enable
	 * 顧客の再有効化
	 */
	@PatchMapping("/admin/customers/{customer_id}/enable")
	public ResponseEntity<Void> enableAdminCustomer(@PathVariable("customer_id") UUID customerId) {
		service.enableActive(customerId, null);
		return ResponseEntity.ok().build();
	}

	/**
	 * DELETE /api/admin/customers/{customer_id}
	 * 顧客の削除
	 */
	@DeleteMapping("/admin/customers/{customer_id}")
	public ResponseEntity<Void> deleteAdminCustomer(@PathVariable("customer_id") UUID customerId) {
		service.delete(customerId, null);
		return ResponseEntity.ok().build();
	}

	/**
	 * GET /api/stores/{store_id}/manager/customers
	 * 顧客一覧取得(店舗内管轄)
	 */
	@GetMapping("/stores/{store_id}/manager/customers")
	public ResponseEntity<Page<CustomerResponse>> getManagerCustomers(
			@PathVariable("store_id") UUID storeId,
			@RequestParam(required = false) @jakarta.validation.constraints.Size(max = 100, message = "Keyword must be less than 100 characters") String name,
			@RequestParam(defaultValue = "created") String sort,
			@RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(value = 0, message = "Page must be 0 or greater") int page,
			@RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(value = 1, message = "Size must be at least 1") @jakarta.validation.constraints.Max(value = 100, message = "Size must not exceed 100") int size) {

		// 現在のユーザーを取得
		User currentUser = securityUtil.getCurrentUserOrThrow();
		
		// マネージャーが自分の店舗のみアクセス可能か確認
		if (currentUser.getStores() == null || 
			currentUser.getStores().stream().noneMatch(store -> store.getId().equals(storeId))) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		Pageable pageable = PageRequest.of(page, size);
		Page<CustomerResponse> customerPage = service.searchCustomers(name, sort, storeId, pageable);
		return ResponseEntity.ok(customerPage);
	}

	/**
	 * GET /api/stores/{store_id}/manager/customers?name={keyword}
	 * 顧客名による検索(店舗内管轄)（上記のgetManagerCustomersメソッドでnameパラメータとして処理）
	 */

	/**
	 * POST /api/stores/{store_id}/manager/customers
	 * 顧客の新規登録(店舗内管轄)
	 */
	@PostMapping("/stores/{store_id}/manager/customers")
	public ResponseEntity<Void> createManagerCustomer(
			@PathVariable("store_id") UUID storeId,
			@Valid @RequestBody CustomerRequest request) {
		// 現在のユーザーを取得
		User currentUser = securityUtil.getCurrentUserOrThrow();
		
		// マネージャーが自分の店舗のみアクセス可能か確認
		if (currentUser.getStores() == null || 
			currentUser.getStores().stream().noneMatch(store -> store.getId().equals(storeId))) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		
		service.create(request);
		return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).build();
	}

	/**
	 * PATCH /api/stores/{store_id}/manager/customers/{customer_id}/disable
	 * 顧客の無効化(店舗内管轄)
	 */
	@PatchMapping("/stores/{store_id}/manager/customers/{customer_id}/disable")
	public ResponseEntity<Void> disableManagerCustomer(
			@PathVariable("store_id") UUID storeId,
			@PathVariable("customer_id") UUID customerId) {
		// 現在のユーザーを取得
		User currentUser = securityUtil.getCurrentUserOrThrow();
		
		// マネージャーが自分の店舗のみアクセス可能か確認
		if (currentUser.getStores() == null || 
			currentUser.getStores().stream().noneMatch(store -> store.getId().equals(storeId))) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		
		service.disableActive(customerId, storeId);
		return ResponseEntity.ok().build();
	}

	/**
	 * PATCH /api/stores/{store_id}/manager/customers/{customer_id}/enable
	 * 顧客の再有効化(店舗内管轄)
	 */
	@PatchMapping("/stores/{store_id}/manager/customers/{customer_id}/enable")
	public ResponseEntity<Void> enableManagerCustomer(
			@PathVariable("store_id") UUID storeId,
			@PathVariable("customer_id") UUID customerId) {
		// 現在のユーザーを取得
		User currentUser = securityUtil.getCurrentUserOrThrow();
		
		// マネージャーが自分の店舗のみアクセス可能か確認
		if (currentUser.getStores() == null || 
			currentUser.getStores().stream().noneMatch(store -> store.getId().equals(storeId))) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		
		service.enableActive(customerId, storeId);
		return ResponseEntity.ok().build();
	}

	/**
	 * DELETE /api/stores/{store_id}/manager/customers/{customer_id}
	 * 顧客の削除(店舗内管轄)
	 */
	@DeleteMapping("/stores/{store_id}/manager/customers/{customer_id}")
	public ResponseEntity<Void> deleteManagerCustomer(
			@PathVariable("store_id") UUID storeId,
			@PathVariable("customer_id") UUID customerId) {
		// 現在のユーザーを取得
		User currentUser = securityUtil.getCurrentUserOrThrow();
		
		// マネージャーが自分の店舗のみアクセス可能か確認
		if (currentUser.getStores() == null || 
			currentUser.getStores().stream().noneMatch(store -> store.getId().equals(storeId))) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		
		service.delete(customerId, storeId);
		return ResponseEntity.ok().build();
	}

	/**
	 * GET /api/stores/{store_id}/trainers/customers
	 * 担当顧客の一覧取得
	 */
	@GetMapping("/stores/{store_id}/trainers/customers")
	public ResponseEntity<java.util.List<CustomerResponse>> getTrainerCustomers(
			@PathVariable("store_id") UUID storeId) {

		// 現在のユーザーを取得
		User currentUser = securityUtil.getCurrentUserOrThrow();
		
		// トレーナーが自分の店舗のみアクセス可能か確認
		if (currentUser.getStores() == null || 
			currentUser.getStores().stream().noneMatch(store -> store.getId().equals(storeId))) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		// 現在ログイン中のトレーナーを取得
		UUID trainerId = currentUser.getId();

		java.util.List<CustomerResponse> customers = service.getCustomersByTrainer(trainerId);
		return ResponseEntity.ok(customers);
	}

	/**
	 * GET /api/customers/{customer_id}/profile
	 * 顧客の基本プロフィール情報取得
	 */
	@GetMapping("/customers/{customer_id}/profile")
	public ResponseEntity<CustomerResponse> getCustomerProfile(@PathVariable("customer_id") UUID customerId) {
		User currentUser = securityUtil.getCurrentUserOrThrow();
		
		// 認可チェック: 顧客にアクセス可能か確認
		if (!customerAuthorizationService.canAccessCustomer(currentUser, customerId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		
		try {
			CustomerResponse customer = service.getCustomerById(customerId);
			if (customer == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok(customer);
		} catch (EntityNotFoundException e) {
			return ResponseEntity.notFound().build();
		} catch (Exception e) {
			log.error("顧客プロフィール取得エラー", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * PATCH /api/customers/{customer_id}/profile
	 * 顧客プロフィールの更新
	 */
	@PatchMapping("/customers/{customer_id}/profile")
	public ResponseEntity<Void> updateCustomerProfile(
			@PathVariable("customer_id") UUID customerId,
			@Valid @RequestBody CustomerRequest request) {
		User currentUser = securityUtil.getCurrentUserOrThrow();
		
		// 認可チェック: 顧客にアクセス可能か確認
		if (!customerAuthorizationService.canAccessCustomer(currentUser, customerId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		
		// storeIdはnull（共通エンドポイントのため）
		service.update(customerId, request, null);
		return ResponseEntity.ok().build();
	}
}