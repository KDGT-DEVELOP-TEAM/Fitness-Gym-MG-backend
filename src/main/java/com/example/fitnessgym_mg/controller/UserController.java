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

import com.example.fitnessgym_mg.dto.request.UserRequest;
import com.example.fitnessgym_mg.dto.response.UserResponse;
import com.example.fitnessgym_mg.service.AccountService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

	private final AccountService service;

	// --- ユーザー一覧（検索・絞り込み・並び替え対応） ---
	@GetMapping
	public String list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String role,
			@RequestParam(defaultValue = "created") String sort,
			Model model) {

		List<UserResponse> users = service.searchUsers(keyword, role, sort);

		model.addAttribute("users", users);
		model.addAttribute("count", users.size());
		model.addAttribute("keyword", keyword);
		model.addAttribute("role", role);
		model.addAttribute("sort", sort);

		return "users/list"; // ← 統一
	}

	// --- 作成 ---
	@PostMapping
	@ResponseBody
	public ResponseEntity<Void> create(
			@Valid @RequestBody UserRequest req) {
		service.create(req);
		return ResponseEntity.ok().build();
	}

	//--- 更新 ---
	@PutMapping("/{id}")
	@ResponseBody
	public ResponseEntity<Void> update(
			@PathVariable UUID id,
			@Valid @RequestBody UserRequest req) {
		service.update(id, req);
		return ResponseEntity.ok().build();
	}

	// --- 有効/無効切替 ---
	@PatchMapping("/{id}/active")
	@ResponseBody
	public ResponseEntity<Void> toggleActive(@PathVariable UUID id) {
		service.toggleActive(id);
		return ResponseEntity.ok().build();
	}

	// --- 削除 ---
	@DeleteMapping("/{id}")
	@ResponseBody
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.delete(id);
		return ResponseEntity.ok().build();
	}
}
