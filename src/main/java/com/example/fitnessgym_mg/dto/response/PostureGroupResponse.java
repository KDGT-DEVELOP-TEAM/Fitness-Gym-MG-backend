package com.example.fitnessgym_mg.dto.response;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.PostureGroup;
import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;
import com.example.fitnessgym_mg.util.DateTimeUtil;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 姿勢グループレスポンスDTO
 * 姿勢画像グループ情報をAPIレスポンスとして返す際に使用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureGroupResponse {

    /**
     * 姿勢グループID
     */
    private UUID id;
    
    /**
     * レッスンID
     */
    private UUID lessonId;
    
    /**
     * レッスン開始日時（Lessonエンティティのstart_dateフィールドから取得）
     */
    private OffsetDateTime lessonStartDate;
    
    /**
     * 撮影日時
     */
    private OffsetDateTime capturedAt;
    
    /**
     * 姿勢画像リスト
     */
    @Builder.Default
    private List<PostureImageResponse> images = new ArrayList<>();
    
    /**
     * 画像の表示順序（front → right → back → left）
     */
    private static final Map<PostureImagePosition, Integer> POSITION_ORDER = Map.of(
            PostureImagePosition.FRONT, 0,
            PostureImagePosition.RIGHT, 1,
            PostureImagePosition.BACK, 2,
            PostureImagePosition.LEFT, 3);
    
    /**
     * PostureGroupエンティティからレスポンスDTOに変換
     * 画像は位置順（front → right → back → left）でソートされる
     * 
     * @param entity PostureGroupエンティティ
     * @return PostureGroupResponse
     */
    public static PostureGroupResponse fromEntity(PostureGroup entity) {
        if (entity == null) {
            return null;
        }
        
        // 画像リストを位置順でソート
        List<PostureImageResponse> sortedImages = new ArrayList<>();
        if (entity.getImages() != null && !entity.getImages().isEmpty()) {
            sortedImages = entity.getImages().stream()
                    .sorted(Comparator.comparingInt(image -> image.getPosition() != null
                            ? POSITION_ORDER.getOrDefault(image.getPosition(), Integer.MAX_VALUE)
                            : Integer.MAX_VALUE))
                    .map(PostureImageResponse::fromEntity)
                    .collect(java.util.stream.Collectors.toList());
        }
        
        return PostureGroupResponse.builder()
                .id(entity.getId())
                .lessonId(entity.getLesson() != null ? entity.getLesson().getId() : null)
                .lessonStartDate(entity.getLesson() != null && entity.getLesson().getStartDate() != null
                        ? DateTimeUtil.toUtcOffset(entity.getLesson().getStartDate())
                        : null)
                .capturedAt(entity.getCapturedAt())
                .images(sortedImages)
                .build();
    }
}

