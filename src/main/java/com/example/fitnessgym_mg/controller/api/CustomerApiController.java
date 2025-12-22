package com.example.fitnessgym_mg.controller.api;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.fitnessgym_mg.dto.request.CustomerRequest;
import com.example.fitnessgym_mg.dto.response.CustomerResponse;
import com.example.fitnessgym_mg.entity.enums.Gender;
import com.example.fitnessgym_mg.exception.AuthenticationException;
import com.example.fitnessgym_mg.service.CustomerService;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CustomerApiController {

	private final CustomerService service;
	private final SecurityUtil securityUtil;

	// --- カスタマー一覧（検索・並び替え対応） ---
	@GetMapping({ "/admin/customers", "/manager/{storeId}/customers" })
	public String list(
			@PathVariable(required = false) UUID storeId, // 店長アクセス時のみ取得
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "created") String sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			Model model) {

		Pageable pageable = PageRequest.of(page, size);

		Page<CustomerResponse> customerPage = service.searchCustomers(keyword, sort, storeId, pageable);

		model.addAttribute("customerPage", customerPage); // PageオブジェクトをViewに渡す
		model.addAttribute("count", customerPage.getTotalElements()); // 総件数
		model.addAttribute("pageNumber", page); // ★ 現在のページ番号（currentPageと競合しないように名前を変更）

		model.addAttribute("keyword", keyword);
		model.addAttribute("sort", sort);
		model.addAttribute("genders", Gender.values());
		model.addAttribute("storeId", storeId);

		// BASE_PATHを設定（Thymeleafテンプレートで使用）
		String basePath = storeId != null ? "/manager/" + storeId + "/customers" : "/admin/customers";
		model.addAttribute("BASE_PATH", basePath);

		// Admin/Manager用サイドバー表示のためのフラグ
		model.addAttribute("isAdmin", storeId == null);
		model.addAttribute("isManager", storeId != null);
		// 現在のページを示すフラグ（サイドバーのアクティブ状態用）
		model.addAttribute("currentPage", "customers");

		return "customer/customer_list";
	}

	// --- 作成 ---
	@PostMapping({ "/admin/customers/create", "/manager/{storeId}/customers/create" })
	@ResponseBody
	public ResponseEntity<Void> create(
			@Valid @RequestBody CustomerRequest req) {

		service.create(req);
		return ResponseEntity.ok().build();
	}

	// --- 編集モーダル表示用にデータ取得 ---
	@GetMapping({ "/admin/customers/{id}/detail", "/manager/{storeId}/customers/{id}/detail" })
	@ResponseBody
	public ResponseEntity<CustomerResponse> getCustomer(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID id) {

		CustomerResponse customer = service.getCustomerById(id);
		return ResponseEntity.ok(customer);
	}

	// --- 更新 ---
	@PutMapping({ "/admin/customers/{id}/edit", "/manager/{storeId}/customers/{id}/edit" })
	@ResponseBody
	public ResponseEntity<Void> update(
			@PathVariable UUID id,
			@PathVariable(required = false) UUID storeId,
			@Valid @RequestBody CustomerRequest req) {

		service.update(id, req, storeId);
		return ResponseEntity.ok().build();
	}

	// 無効化 (Disable)
	@PatchMapping({ "/admin/customers/{id}/disable", "/manager/{storeId}/customers/{id}/disable" })
	@ResponseBody
	public ResponseEntity<Void> disableCustomer(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID id) {

		service.disableActive(id, storeId);
		return ResponseEntity.ok().build();
	}

	// 有効化 (Enable)
	@PatchMapping({ "/admin/customers/{id}/enable", "/manager/{storeId}/customers/{id}/enable" })
	@ResponseBody
	public ResponseEntity<Void> enableCustomer(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID id) {

		service.enableActive(id, storeId);
		return ResponseEntity.ok().build();
	}

	// --- 削除 ---
	@DeleteMapping({ "/admin/customers/{id}/delete", "/manager/{storeId}/customers/{id}/delete" })
	@ResponseBody
	public ResponseEntity<Void> delete(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID id) {

		service.delete(id, storeId);
		return ResponseEntity.ok().build();
	}

	// --- 顧客レッスン履歴画面 ---
	// パスは admin と manager の両方を受け付け、LessonController.lessonHistory にリダイレクト
	@GetMapping({ "/admin/customers/{id}/lessons", "/manager/{storeId}/customers/{id}/lessons" })
	public String showCustomerLessons(
			@PathVariable(required = false) UUID storeId, // storeIdはURLに含まれるため取得
			@PathVariable UUID id) {

		// LessonController.lessonHistory にリダイレクト
		if (storeId != null) {
			return "redirect:/manager/" + storeId + "/history/" + id;
		} else {
			return "redirect:/admin/history/" + id;
		}
	}

	// ========== REST API エンドポイント（React用） ==========
	// 注意: このクラスはWebコントローラーとREST APIコントローラーが混在しているため、
	// @RequestMapping("/api")をクラスレベルで設定できない
	// そのため、各メソッドで完全なパスを指定している

	/**
	 * GET /api/admin/customers
	 * 顧客一覧取得
	 */
	@GetMapping("/api/admin/customers")
	@ResponseBody
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
	@PostMapping("/api/admin/customers")
	@ResponseBody
	public ResponseEntity<Void> createAdminCustomer(@Valid @RequestBody CustomerRequest request) {
		service.create(request);
		return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).build();
	}

	/**
	 * PATCH /api/admin/customers/{customer_id}/disable
	 * 顧客の無効化
	 */
	@PatchMapping("/api/admin/customers/{customer_id}/disable")
	@ResponseBody
	public ResponseEntity<Void> disableAdminCustomer(@PathVariable("customer_id") UUID customerId) {
		service.disableActive(customerId, null);
		return ResponseEntity.ok().build();
	}

	/**
	 * PATCH /api/admin/customers/{customer_id}/enable
	 * 顧客の再有効化
	 */
	@PatchMapping("/api/admin/customers/{customer_id}/enable")
	@ResponseBody
	public ResponseEntity<Void> enableAdminCustomer(@PathVariable("customer_id") UUID customerId) {
		service.enableActive(customerId, null);
		return ResponseEntity.ok().build();
	}

	/**
	 * DELETE /api/admin/customers/{customer_id}
	 * 顧客の削除
	 */
	@DeleteMapping("/api/admin/customers/{customer_id}")
	@ResponseBody
	public ResponseEntity<Void> deleteAdminCustomer(@PathVariable("customer_id") UUID customerId) {
		service.delete(customerId, null);
		return ResponseEntity.ok().build();
	}

	/**
	 * GET /api/stores/{store_id}/manager/customers
	 * 顧客一覧取得(店舗内管轄)
	 */
	@GetMapping("/api/stores/{store_id}/manager/customers")
	@ResponseBody
	public ResponseEntity<Page<CustomerResponse>> getManagerCustomers(
			@PathVariable("store_id") UUID storeId,
			@RequestParam(required = false) @jakarta.validation.constraints.Size(max = 100, message = "Keyword must be less than 100 characters") String name,
			@RequestParam(defaultValue = "created") String sort,
			@RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(value = 0, message = "Page must be 0 or greater") int page,
			@RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(value = 1, message = "Size must be at least 1") @jakarta.validation.constraints.Max(value = 100, message = "Size must not exceed 100") int size) {

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
	@PostMapping("/api/stores/{store_id}/manager/customers")
	@ResponseBody
	public ResponseEntity<Void> createManagerCustomer(
			@PathVariable("store_id") UUID storeId,
			@Valid @RequestBody CustomerRequest request) {
		service.create(request);
		return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).build();
	}

	/**
	 * PATCH /api/stores/{store_id}/manager/customers/{customer_id}/disable
	 * 顧客の無効化(店舗内管轄)
	 */
	@PatchMapping("/api/stores/{store_id}/manager/customers/{customer_id}/disable")
	@ResponseBody
	public ResponseEntity<Void> disableManagerCustomer(
			@PathVariable("store_id") UUID storeId,
			@PathVariable("customer_id") UUID customerId) {
		service.disableActive(customerId, storeId);
		return ResponseEntity.ok().build();
	}

	/**
	 * PATCH /api/stores/{store_id}/manager/customers/{customer_id}/enable
	 * 顧客の再有効化(店舗内管轄)
	 */
	@PatchMapping("/api/stores/{store_id}/manager/customers/{customer_id}/enable")
	@ResponseBody
	public ResponseEntity<Void> enableManagerCustomer(
			@PathVariable("store_id") UUID storeId,
			@PathVariable("customer_id") UUID customerId) {
		service.enableActive(customerId, storeId);
		return ResponseEntity.ok().build();
	}

	/**
	 * DELETE /api/stores/{store_id}/manager/customers/{customer_id}
	 * 顧客の削除(店舗内管轄)
	 */
	@DeleteMapping("/api/stores/{store_id}/manager/customers/{customer_id}")
	@ResponseBody
	public ResponseEntity<Void> deleteManagerCustomer(
			@PathVariable("store_id") UUID storeId,
			@PathVariable("customer_id") UUID customerId) {
		service.delete(customerId, storeId);
		return ResponseEntity.ok().build();
	}

	/**
	 * GET /api/stores/{store_id}/trainers/customers
	 * 担当顧客の一覧取得
	 */
	@GetMapping("/api/stores/{store_id}/trainers/customers")
	@ResponseBody
	public ResponseEntity<java.util.List<CustomerResponse>> getTrainerCustomers(
			@PathVariable("store_id") UUID storeId) {

		// 現在ログイン中のトレーナーを取得
		UUID trainerId = securityUtil.getCurrentUserOrThrow().getId();

		java.util.List<CustomerResponse> customers = service.getCustomersByTrainer(trainerId);
		return ResponseEntity.ok(customers);
	}

	/**
	 * GET /api/customers/{customer_id}/profile
	 * 顧客の基本プロフィール情報取得
	 */
	@GetMapping("/api/customers/{customer_id}/profile")
	@ResponseBody
	public ResponseEntity<CustomerResponse> getCustomerProfile(@PathVariable("customer_id") UUID customerId) {
		CustomerResponse customer = service.getCustomerById(customerId);
		return ResponseEntity.ok(customer);
	}

	/**
	 * PATCH /api/customers/{customer_id}/profile
	 * 顧客プロフィールの更新
	 */
	@PatchMapping("/api/customers/{customer_id}/profile")
	@ResponseBody
	public ResponseEntity<Void> updateCustomerProfile(
			@PathVariable("customer_id") UUID customerId,
			@Valid @RequestBody CustomerRequest request) {
		// storeIdはnull（共通エンドポイントのため）
		service.update(customerId, request, null);
		return ResponseEntity.ok().build();
	}
}