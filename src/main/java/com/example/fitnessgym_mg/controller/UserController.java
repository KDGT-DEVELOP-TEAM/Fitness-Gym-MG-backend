package com.example.fitnessgym_mg.controller;

import java.util.Collections; // Collections.emptySet() のために追加
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.repository.StoreRepository;
import com.example.fitnessgym_mg.service.AccountService;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserController {

	private final AccountService service;
	private final StoreRepository storeRepository;
	private final SecurityUtil securityUtil;

	// --- ユーザー一覧（本部管理者 /admin/users, 店長 /manager/{storeId}/users）---
	@GetMapping({ "/admin/users", "/manager/{storeId}/users" })
	public String list(
			@PathVariable(required = false) UUID storeId,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String role,
			@RequestParam(defaultValue = "created") String sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			Model model) {

		Pageable pageable = PageRequest.of(page, size);

		Page<UserResponse> userPage = service.searchUsers(keyword, role, sort, storeId, pageable);

		// 新規作成/編集モーダルのための全店舗リストを取得
		List<Store> allStores = storeRepository.findAll();
		model.addAttribute("stores", allStores);

		model.addAttribute("userPage", userPage);
		// ★ カウント情報をUserPageから取得する ★
		model.addAttribute("count", userPage.getTotalElements());
		model.addAttribute("currentPage", page);
		model.addAttribute("keyword", keyword);
		model.addAttribute("role", role);
		model.addAttribute("sort", sort);
		model.addAttribute("storeId", storeId);
		
		// 店長用サイドバー表示のためのフラグ（storeIdが存在する場合のみ）
		model.addAttribute("isManager", storeId != null);
		// 現在のページを示すフラグ（サイドバーのアクティブ状態用）
		if (storeId != null) {
			model.addAttribute("currentPage", "users");
		}

		return "user/user-list";
	}

	// --- 作成 (API) ---
	@PostMapping({ "/admin/users/create", "/manager/{storeId}/users/create" })
	@ResponseBody
	public ResponseEntity<String> create(
			@PathVariable(required = false) UUID pathStoreId,
			@Valid @RequestBody UserRequest req) {

		try {
			// null チェックと空セットの提供
			service.create(req, req.getStoreIds() != null ? req.getStoreIds() : Collections.emptySet());
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			// 権限エラーやバリデーションエラーの場合、403エラーとして返す
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		}
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
	public ResponseEntity<String> update(
			@PathVariable(required = false) UUID pathStoreId,
			@PathVariable UUID id,
			@Valid @RequestBody UserRequest req) {

		try {
			// null チェックと空セットの提供
			service.update(id, req, req.getStoreIds() != null ? req.getStoreIds() : Collections.emptySet());
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			// 権限エラーやバリデーションエラーの場合、403エラーとして返す
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		}
	}

	// --- ユーザーを無効化 (Disable) ---
	@PatchMapping({ "/admin/users/{id}/disable", "/manager/{storeId}/users/{id}/disable" })
	@ResponseBody
	public ResponseEntity<String> disableUser(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID id) {

		try {
			service.disableActive(id, storeId);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			// 権限エラーやバリデーションエラーの場合、403エラーとして返す
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		}
	}

	// --- ユーザーを有効化 (Enable) ---
	@PatchMapping({ "/admin/users/{id}/enable", "/manager/{storeId}/users/{id}/enable" })
	@ResponseBody
	public ResponseEntity<String> enableUser(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID id) {

		try {
			service.enableActive(id, storeId);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			// 権限エラーやバリデーションエラーの場合、403エラーとして返す
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		}
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