package com.example.fitnessgym_mg.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * 姿勢画像グループ関連のREST APIコントローラー
 * 姿勢画像グループのデータ取得APIを提供
 */
@RestController
@RequestMapping("/api/customers/{customerId}/posture_groups")
@RequiredArgsConstructor
public class PostureGroupController {

    /**
     * 姿勢画像の位置順序（front → right → back → left）
     * 画像を表示する際の順序を定義
     */
    private static final Map<PostureImagePosition, Integer> POSITION_ORDER = Map.of(
            PostureImagePosition.FRONT, 0,
            PostureImagePosition.RIGHT, 1,
            PostureImagePosition.BACK, 2,
            PostureImagePosition.LEFT, 3
    );

    private final PostureGroupService postureGroupService;

    /**
     * 指定された顧客の姿勢画像グループ一覧を取得
     * GET /api/customers/{customerId}/posture_groups
     * @param customerId 顧客ID
     * @return 姿勢画像グループのレスポンスリスト（JSON形式）
     */
    @GetMapping
    public ResponseEntity<List<PostureGroupResponse>> listGroups(@PathVariable UUID customerId) {
        List<PostureGroupResponse> response = postureGroupService.findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * 姿勢画像グループエンティティをレスポンスDTOに変換
     * @param postureGroup 姿勢画像グループエンティティ
     * @return 姿勢画像グループレスポンスDTO
     */
    private PostureGroupResponse toResponse(PostureGroup postureGroup) {
        return PostureGroupResponse.builder()
                .id(postureGroup.getId())
                .lessonId(postureGroup.getLesson() != null ? postureGroup.getLesson().getId() : null)
                .lessonStartDate(postureGroup.getLesson() != null ? postureGroup.getLesson().getStartDate() : null)
                .capturedAt(postureGroup.getCapturedAt())
                .images(toImageResponses(postureGroup.getImages()))
                .build();
    }

    /**
     * 姿勢画像エンティティリストをレスポンスDTOリストに変換
     * 画像は位置順（front → right → back → left）でソート
     * @param images 姿勢画像エンティティリスト
     * @return 姿勢画像レスポンスDTOリスト
     */
    private List<PostureImageResponse> toImageResponses(List<PostureImage> images) {
        return images.stream()
                .sorted(Comparator.comparingInt(image -> POSITION_ORDER.getOrDefault(image.getPosition(), Integer.MAX_VALUE)))
                .map(image -> PostureImageResponse.builder()
                        .id(image.getId())
                        .title(image.getTitle())
                        .storageKey(image.getStorageKey())
                        .consentPublication(image.isConsentPublication())
                        .takenAt(image.getTakenAt())
                        .position(image.getPosition() != null ? image.getPosition().getCode() : null)
                        .build())
                .collect(Collectors.toList());
    }
}

