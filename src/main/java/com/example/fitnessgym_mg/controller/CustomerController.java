package com.example.fitnessgym_mg.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

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
import org.springframework.web.bind.annotation.RequestMapping;
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
@RequestMapping("/customers")
public class CustomerController {

	private final CustomerService service;

	/**
	 * --- カスタマー一覧（検索・並び替え対応） ---
	 */
	@GetMapping
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "created") String sort,
			Model model) {

		List<CustomerResponse> customers = service.searchCustomers(keyword, sort);

		model.addAttribute("customers", customers);
		model.addAttribute("count", customers.size());
		model.addAttribute("keyword", keyword);
		model.addAttribute("sort", sort);
		model.addAttribute("genders", CustomerGender.values()); // モーダル用にEnumを渡す

		return "customers/list";
	}

	/**
	 * --- 作成 ---
	 */
	@PostMapping
	@ResponseBody
	public ResponseEntity<Void> create(
			@Valid @RequestBody CustomerRequest req) {
		service.create(req);
		return ResponseEntity.ok().build();
	}

	/**
	 * --- 編集モーダル表示用にデータ取得 ---
	 */
	@GetMapping("/{id}")
	@ResponseBody
	public ResponseEntity<Customer> getCustomer(@PathVariable UUID id) {
		// CustomerRequestではなく、エンティティそのものを返すことで全情報をクライアントに渡す
		Customer customer = service.findById(id);
		return ResponseEntity.ok(customer);
	}

	/**
	 * --- 更新 ---
	 */
	@PutMapping("/{id}")
	@ResponseBody
	public ResponseEntity<Void> update(
			@PathVariable UUID id,
			@Valid @RequestBody CustomerRequest req) {
		service.update(id, req);
		return ResponseEntity.ok().build();
	}

	/**
	 * --- 有効/無効切替 ---
	 */
	@PatchMapping("/{id}/active")
	@ResponseBody
	public ResponseEntity<Void> toggleActive(@PathVariable UUID id) {
		service.toggleActive(id);
		return ResponseEntity.ok().build();
	}

	/**
	 * --- 削除 ---
	 */
	@DeleteMapping("/{id}")
	@ResponseBody
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.delete(id);
		return ResponseEntity.ok().build();
	}
}