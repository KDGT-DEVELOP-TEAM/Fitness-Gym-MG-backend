package com.example.fitnessgym_mg.controller.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fitnessgym_mg.dto.response.PostureGroupResponse;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.service.PostureGroupService;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

/**
 * 姿勢画像グループREST APIエンドポイント
 * Controllerの責務: HTTPリクエスト/レスポンスの制御のみ。
 * 認可チェックとDTO変換はService層で実施される。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostureGroupApiController {

	private final PostureGroupService postureGroupService;
	private final SecurityUtil securityUtil;

	/**
	 * GET /api/customers/{customer_id}/posture_groups
	 * 顧客の姿勢画像グループ一覧をJSON形式で返す
	 * 
	 * <p>Controllerの責務: HTTPリクエスト/レスポンスの制御のみ。
	 * 認可チェックとDTO変換はService層で実施される。</p>
	 */
	@GetMapping("/customers/{customer_id}/posture_groups")
	public ResponseEntity<List<PostureGroupResponse>> listGroups(@PathVariable("customer_id") UUID customerId) {
		// 現在のユーザーを取得
		User currentUser = securityUtil.getCurrentUserOrThrow();
		
		// Service層で認可チェックとDTO変換を実施
		List<PostureGroupResponse> response = postureGroupService.findByCustomerIdWithAuth(currentUser, customerId);
		
		return ResponseEntity.ok(response);
	}

	/**
	 * POST /api/lessons/{lesson_id}/posture_groups
	 * 新しいレッスン記録に伴う、新しい姿勢画像群に対する空グループの作成
	 * 
	 * <p>Controllerの責務: HTTPリクエスト/レスポンスの制御のみ。
	 * 認可チェックとDTO変換はService層で実施される。</p>
	 */
	@PostMapping("/lessons/{lesson_id}/posture_groups")
	public ResponseEntity<PostureGroupResponse> createPostureGroup(@PathVariable("lesson_id") UUID lessonId) {
		// 現在のユーザーを取得
		User currentUser = securityUtil.getCurrentUserOrThrow();
		
		// Service層で認可チェック、作成、DTO変換を実施
		PostureGroupResponse response = postureGroupService.createPostureGroupWithAuth(currentUser, lessonId);
		
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
