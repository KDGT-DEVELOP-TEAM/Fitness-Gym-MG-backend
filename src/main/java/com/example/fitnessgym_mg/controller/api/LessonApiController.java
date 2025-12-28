package com.example.fitnessgym_mg.controller.api;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.service.AuthorizationFacade;
import com.example.fitnessgym_mg.service.LessonService;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

/**
 * レッスン管理REST APIコントローラー
 * すべてのエンドポイントはJSONを返します
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LessonApiController {

	private final LessonService lessonService;
	private final SecurityUtil securityUtil;
	private final AuthorizationFacade authorizationFacade;

	// ========== REST API エンドポイント ==========

	/**
	 * POST /api/customers/{customer_id}/lessons
	 * 新しいレッスン記録の作成
	 */
	@PostMapping("/customers/{customer_id}/lessons")
	public ResponseEntity<com.example.fitnessgym_mg.dto.response.LessonResponse> createLesson(
			@PathVariable("customer_id") UUID customerId,
			@Valid @RequestBody com.example.fitnessgym_mg.dto.request.LessonRequest request) {

		// 現在のユーザーを取得
		User currentUser = securityUtil.getCurrentUserOrThrow();
		
		// 認可チェック: 操作者がその顧客に対して権限を持つか確認
		if (!authorizationFacade.canAccessCustomer(currentUser, customerId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		// customerIdを引数として渡す（request.setCustomerId()を削除）
		com.example.fitnessgym_mg.entity.Lesson savedLesson = lessonService.createLesson(customerId, request);
		com.example.fitnessgym_mg.dto.response.LessonResponse response = lessonService
				.getLessonDetail(savedLesson.getId());

		return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
	}

	/**
	 * GET /api/customers/{customer_id}/lessons
	 * 顧客の全レッスン履歴一覧（ページネーション/フィルタリング）
	 */
	@GetMapping("/customers/{customer_id}/lessons")
	public ResponseEntity<org.springframework.data.domain.Page<com.example.fitnessgym_mg.dto.response.LessonResponse>> getCustomerLessons(
			@PathVariable("customer_id") UUID customerId,
			@RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(value = 0, message = "Page must be 0 or greater") int page,
			@RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(value = 1, message = "Size must be at least 1") @jakarta.validation.constraints.Max(value = 100, message = "Size must not exceed 100") int size) {

		// 現在のユーザーを取得
		User currentUser = securityUtil.getCurrentUserOrThrow();
		
		// 認可チェック: 操作者がその顧客に対して権限を持つか確認
		if (!authorizationFacade.canAccessCustomer(currentUser, customerId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
		org.springframework.data.domain.Page<com.example.fitnessgym_mg.dto.response.LessonResponse> lessonPage = lessonService
				.getLessonsByCustomerId(customerId, pageable);

		return ResponseEntity.ok(lessonPage);
	}

	/**
	 * GET /api/lessons/{lesson_id}
	 * 特定のレッスン記録の詳細情報取得
	 */
	@GetMapping("/lessons/{lesson_id}")
	public ResponseEntity<com.example.fitnessgym_mg.dto.response.LessonResponse> getLesson(
			@PathVariable("lesson_id") UUID lessonId) {

		// 現在のユーザーを取得
		User currentUser = securityUtil.getCurrentUserOrThrow();
		
		// 認可チェック: 操作者がそのレッスンにアクセス可能か確認
		if (!authorizationFacade.canAccessLesson(currentUser, lessonId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		// 認可チェックはService層でも実施されるが、Controller層で早期リターン
		com.example.fitnessgym_mg.dto.response.LessonResponse lesson = lessonService.getLessonDetail(lessonId);
		return ResponseEntity.ok(lesson);
	}

	/**
	 * PATCH /api/lessons/{lesson_id}
	 * 既存レッスン情報の編集
	 */
	@PatchMapping("/lessons/{lesson_id}")
	public ResponseEntity<com.example.fitnessgym_mg.dto.response.LessonResponse> updateLesson(
			@PathVariable("lesson_id") UUID lessonId,
			@Valid @RequestBody com.example.fitnessgym_mg.dto.request.LessonRequest request) {

		// 現在のユーザーを取得
		User currentUser = securityUtil.getCurrentUserOrThrow();
		
		// 認可チェック: 操作者がそのレッスンにアクセス可能か確認
		if (!authorizationFacade.canAccessLesson(currentUser, lessonId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		// 認可チェックはService層でも実施されるが、Controller層で早期リターン
		com.example.fitnessgym_mg.dto.response.LessonResponse response = lessonService.updateLesson(lessonId, request);
		return ResponseEntity.ok(response);
	}
}