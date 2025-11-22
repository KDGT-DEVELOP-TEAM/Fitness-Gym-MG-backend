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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.fitnessgym_mg.dto.request.UserRequest;
import com.example.fitnessgym_mg.dto.response.UserResponse;
import com.example.fitnessgym_mg.service.AccountService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserController {

	private final AccountService service;

	// --- ユーザー一覧（本部管理者 /admin/users, 店長 /manager/{storeId}/users）---
	@GetMapping({ "/admin/users", "/manager/{storeId}/users" })
	public String list(
			@PathVariable(required = false) UUID storeId, // 店長アクセス時のみ取得
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String role,
			@RequestParam(defaultValue = "created") String sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			Model model) {

		Pageable pageable = PageRequest.of(page, size);

		Page<UserResponse> userPage = service.searchUsers(keyword, role, sort, storeId, pageable);

		model.addAttribute("userPage", userPage);
		model.addAttribute("currentPage", page);
		model.addAttribute("keyword", keyword);
		model.addAttribute("role", role);
		model.addAttribute("sort", sort);
		model.addAttribute("storeId", storeId); // Viewに渡す

		return "users/list";
	}

	// --- 作成 (API) ---
	@PostMapping({ "/admin/users/create", "/manager/{storeId}/users/create" })
	@ResponseBody
	public ResponseEntity<Void> create(
			@PathVariable(required = false) UUID pathStoreId,
			@Valid @RequestBody UserRequest req,
			@RequestParam(required = false) UUID storeId) { // 仮にクエリパラメータで受け取る場合

		service.create(req, storeId);
		return ResponseEntity.ok().build();
	}

	// --- 詳細表示 (API) ---
	@GetMapping({ "/admin/users/{id}/detail", "/manager/{storeId}/users/{id}/detail" })
	@ResponseBody
	public UserResponse getUser(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID id) {

		return service.findById(id, storeId);
	}

	//--- 更新 (API) ---
	@PatchMapping({ "/admin/users/{id}/edit", "/manager/{storeId}/users/{id}/edit" })
	@ResponseBody
	public ResponseEntity<Void> update(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID id,
			@Valid @RequestBody UserRequest req) {

		service.update(id, req, storeId);
		return ResponseEntity.ok().build();
	}

	// --- ユーザーを無効化 (Disable) ---
	@PatchMapping({ "/admin/users/{id}/disable", "/manager/{storeId}/users/{id}/disable" })
	@ResponseBody
	public ResponseEntity<Void> disableUser(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID id) {

		// サービス層の無効化メソッドを呼び出す
		service.disableActive(id, storeId);
		return ResponseEntity.ok().build();
	}

	// --- ユーザーを有効化 (Enable) ---
	@PatchMapping({ "/admin/users/{id}/enable", "/manager/{storeId}/users/{id}/enable" })
	@ResponseBody
	public ResponseEntity<Void> enableUser(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID id) {

		// サービス層の有効化メソッドを呼び出す
		service.enableActive(id, storeId);
		return ResponseEntity.ok().build();
	}

	// --- 削除 (API) ---
	@DeleteMapping({ "/admin/users/{id}/delete", "/manager/{storeId}/users/{id}/delete" })
	@ResponseBody
	public ResponseEntity<Void> delete(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID id) {

		service.delete(id, storeId);
		return ResponseEntity.ok().build();
	}
}