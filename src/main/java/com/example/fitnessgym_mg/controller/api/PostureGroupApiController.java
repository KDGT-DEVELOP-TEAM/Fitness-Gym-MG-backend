package com.example.fitnessgym_mg.controller.api;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fitnessgym_mg.dto.response.PostureGroupResponse;
import com.example.fitnessgym_mg.dto.response.PostureImageResponse;
import com.example.fitnessgym_mg.entity.PostureGroup;
import com.example.fitnessgym_mg.entity.PostureImage;
import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;
import com.example.fitnessgym_mg.service.PostureGroupService;

import lombok.RequiredArgsConstructor;

/**
 * 姿勢画像グループREST APIエンドポイント
 * DBから取得したエンティティをJSON形式で返す
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostureGroupApiController {

	// 画像の表示順序（front → right → back → left）
	private static final Map<PostureImagePosition, Integer> POSITION_ORDER = Map.of(
			PostureImagePosition.FRONT, 0,
			PostureImagePosition.RIGHT, 1,
			PostureImagePosition.BACK, 2,
			PostureImagePosition.LEFT, 3);

	private final PostureGroupService postureGroupService;

	/**
	 * GET /api/customers/{customer_id}/posture_groups
	 * 顧客の姿勢画像グループ一覧をJSON形式で返す
	 * フロー: DB取得 → Entity → DTO変換 → JSON
	 */
	@GetMapping("/customers/{customer_id}/posture_groups")
	public ResponseEntity<List<PostureGroupResponse>> listGroups(@PathVariable("customer_id") UUID customerId) {
		List<PostureGroupResponse> response = postureGroupService.findByCustomerId(customerId)
				.stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
		return ResponseEntity.ok(response);
	}

	// Entity → DTO変換（クライアント向けJSONデータ構造に変換）
	private PostureGroupResponse toResponse(PostureGroup postureGroup) {
		OffsetDateTime lessonStartDate = null;
		if (postureGroup.getLesson() != null && postureGroup.getLesson().getStartDate() != null) {
			// LocalDateTimeをOffsetDateTimeに変換（システムのデフォルトタイムゾーンを使用）
			LocalDateTime localDateTime = postureGroup.getLesson().getStartDate();
			lessonStartDate = localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
		}

		return PostureGroupResponse.builder()
				.id(postureGroup.getId())
				.lessonId(postureGroup.getLesson() != null ? postureGroup.getLesson().getId() : null)
				.lessonStartDate(lessonStartDate)
				.capturedAt(postureGroup.getCapturedAt())
				.images(toImageResponses(postureGroup.getImages()))
				.build();
	}

	// 画像リストをDTO変換し位置順でソート
	private List<PostureImageResponse> toImageResponses(List<PostureImage> images) {
		if (images == null || images.isEmpty()) {
			return List.of();
		}
		return images.stream()
				.sorted(Comparator.comparingInt(image -> image.getPosition() != null
						? POSITION_ORDER.getOrDefault(image.getPosition(), Integer.MAX_VALUE)
						: Integer.MAX_VALUE))
				.map(this::toImageResponse)
				.collect(Collectors.toList());
	}

	// PostureImageエンティティからDTOに変換（nullセーフ）
	private PostureImageResponse toImageResponse(PostureImage image) {
		if (image == null) {
			return null;
		}
		return PostureImageResponse.builder()
				.id(image.getId())
				.storageKey(image.getStorageKey())
				.consentPublication(image.isConsentPublication())
				.takenAt(image.getTakenAt())
				.position(image.getPosition() != null ? image.getPosition().getCode() : null)
				.build();
	}

	/**
	 * POST /api/lessons/{lesson_id}/posture_groups
	 * 新しいレッスン記録に伴う、新しい姿勢画像群に対する空グループの作成
	 */
	@PostMapping("/lessons/{lesson_id}/posture_groups")
	public ResponseEntity<PostureGroupResponse> createPostureGroup(@PathVariable("lesson_id") UUID lessonId) {
		PostureGroup postureGroup = postureGroupService.createPostureGroup(lessonId);
		PostureGroupResponse response = toResponse(postureGroup);
		return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
	}
}
