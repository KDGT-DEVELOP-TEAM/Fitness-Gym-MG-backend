package com.example.fitnessgym_mg.controller.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.example.fitnessgym_mg.dto.response.LessonResponse;
import com.example.fitnessgym_mg.service.LessonService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * レッスン管理REST APIコントローラー
 * すべてのエンドポイントはJSONを返します
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LessonApiController {

	private final LessonService lessonService;

	// ========== REST API エンドポイント ==========

	/**
	 * POST /api/customers/{customer_id}/lessons
	 * 新しいレッスン記録の作成
	 */
	@PreAuthorize("@authorizationFacade.canAccessCustomer(authentication, #customerId)")
	@PostMapping("/customers/{customer_id}/lessons")
	public ResponseEntity<com.example.fitnessgym_mg.dto.response.LessonResponse> createLesson(
			@PathVariable("customer_id") UUID customerId,
			@Valid @RequestBody com.example.fitnessgym_mg.dto.request.LessonRequest request) {

		// customerIdを引数として渡す（request.setCustomerId()を削除）
		com.example.fitnessgym_mg.entity.Lesson savedLesson = lessonService.createLesson(customerId, request);
		com.example.fitnessgym_mg.dto.response.LessonResponse response = lessonService
				.getLessonDetail(savedLesson.getId());

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * GET /api/customers/{customer_id}/lessons
	 * 顧客の全レッスン履歴一覧（ページネーション/フィルタリング）
	 */
	@PreAuthorize("@authorizationFacade.canAccessCustomer(authentication, #customerId)")
	@GetMapping("/customers/{customer_id}/lessons")
	public ResponseEntity<Page<LessonResponse>> getCustomerLessons(
			@PathVariable("customer_id") UUID customerId,
			@RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(value = 0, message = "Page must be 0 or greater") int page,
			@RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(value = 1, message = "Size must be at least 1") @jakarta.validation.constraints.Max(value = 100, message = "Size must not exceed 100") int size) {

		Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
		Page<LessonResponse> lessonPage = lessonService
				.getLessonsByCustomerId(customerId, pageable);

		return ResponseEntity.ok(lessonPage);
	}

	/**
	 * GET /api/lessons/{lesson_id}
	 * 特定のレッスン記録の詳細情報取得
	 */
	@PreAuthorize("@authorizationFacade.canAccessLesson(authentication, #lessonId)")
	@GetMapping("/lessons/{lesson_id}")
	public ResponseEntity<com.example.fitnessgym_mg.dto.response.LessonResponse> getLesson(
			@PathVariable("lesson_id") UUID lessonId) {

		// 認可チェックはService層でも実施されるが、Controller層で早期リターン
		com.example.fitnessgym_mg.dto.response.LessonResponse lesson = lessonService.getLessonDetail(lessonId);
		return ResponseEntity.ok(lesson);
	}

	/**
	 * PATCH /api/lessons/{lesson_id}
	 * 既存レッスン情報の編集
	 */
	@PreAuthorize("@authorizationFacade.canAccessLesson(authentication, #lessonId)")
	@PatchMapping("/lessons/{lesson_id}")
	public ResponseEntity<com.example.fitnessgym_mg.dto.response.LessonResponse> updateLesson(
			@PathVariable("lesson_id") UUID lessonId,
			@Valid @RequestBody com.example.fitnessgym_mg.dto.request.LessonRequest request) {

		// 認可チェックはService層でも実施されるが、Controller層で早期リターン
		com.example.fitnessgym_mg.dto.response.LessonResponse response = lessonService.updateLesson(lessonId, request);
		return ResponseEntity.ok(response);
	}

	/**
	 * GET /api/lessons/next-by-trainer/{trainer_id}
	 * トレーナー別の次回レッスン希望日程一覧取得
	 * 
	 * <p>指定されたトレーナーの次回レッスン希望日程（nextDateが設定されているレッスン）を取得します。</p>
	 * <p>nextDateが未来の日時のレッスンのみを返します。</p>
	 * 
	 * @param trainerId トレーナーID
	 * @return 次回レッスン希望日程一覧
	 */
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TRAINER')")
	@GetMapping("/lessons/next-by-trainer/{trainer_id}")
	public ResponseEntity<List<LessonResponse>> getNextLessonsByTrainer(
			@PathVariable("trainer_id") UUID trainerId) {
		
		log.debug("トレーナー別次回レッスン希望日程一覧取得リクエスト: trainerId={}", trainerId);
		
		List<LessonResponse> responses = lessonService.getNextLessonsByTrainerIdWithoutPaging(trainerId);
		
		log.info("トレーナー別次回レッスン希望日程一覧取得成功: trainerId={}, count={}", trainerId, responses.size());
		return ResponseEntity.ok(responses);
	}
}