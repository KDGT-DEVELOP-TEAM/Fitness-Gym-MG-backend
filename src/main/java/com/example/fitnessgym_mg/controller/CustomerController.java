package com.example.fitnessgym_mg.controller;

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
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.Customer.CustomerGender;
import com.example.fitnessgym_mg.service.CustomerService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService service;

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
		model.addAttribute("currentPage", page); // ★ 現在のページ番号

		model.addAttribute("keyword", keyword);
		model.addAttribute("sort", sort);
		model.addAttribute("genders", CustomerGender.values());
		model.addAttribute("storeId", storeId);

		return "customers/list";
	}

	// --- 作成 ---
	@PostMapping({ "/admin/customers/create", "/manager/{storeId}/customers/create" })
	@ResponseBody
	public ResponseEntity<Void> create(
			@PathVariable(required = false) UUID storeId, // 店長の場合に取得
			@Valid @RequestBody CustomerRequest req) {

		service.create(req, storeId);
		return ResponseEntity.ok().build();
	}

	// --- 編集モーダル表示用にデータ取得 ---
	@GetMapping({ "/admin/customers/{id}/detail", "/manager/{storeId}/customers/{id}/detail" })
	@ResponseBody
	public ResponseEntity<Customer> getCustomer(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID id) {

		Customer customer = service.findById(id, storeId);
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
	// パスは admin と manager の両方を受け付け、View名は一つに統一
	@GetMapping({ "/admin/customers/{id}/lessons", "/manager/{storeId}/customers/{id}/lessons" })
	public String showCustomerLessons(
			@PathVariable(required = false) UUID storeId, // storeIdはURLに含まれるため取得
			@PathVariable UUID id,
			Model model) {

		// TODO: LessonServiceなどを使用して、この顧客ID (id) に紐づくレッスン履歴を取得する処理を実装

		// ここで storeId が null かどうかで、誰がアクセスしているかを識別
		model.addAttribute("customerId", id);
		model.addAttribute("storeId", storeId); // nullの場合もある

		// View内で戻るボタンなどのリンクを構築しやすくするためにBASE_PATHを渡す
		model.addAttribute("BASE_PATH", (storeId != null ? "/manager/" + storeId : "/admin") + "/customers");

		// 遷移先のビュー名は一つに統一
		return "customers/lessons";
	}
}