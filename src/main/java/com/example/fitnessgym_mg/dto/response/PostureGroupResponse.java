package com.example.fitnessgym_mg.dto.response;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.PostureGroup;

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
     * PostureGroupエンティティからレスポンスDTOに変換
     * 
     * @param entity PostureGroupエンティティ
     * @return PostureGroupResponse
     */
    public static PostureGroupResponse fromEntity(PostureGroup entity) {
        if (entity == null) {
            return null;
        }
        
        return PostureGroupResponse.builder()
                .id(entity.getId())
                .lessonId(entity.getLesson() != null ? entity.getLesson().getId() : null)
                .lessonStartDate(entity.getLesson() != null && entity.getLesson().getStartDate() != null
                        ? entity.getLesson().getStartDate().atZone(ZoneId.systemDefault()).toOffsetDateTime()
                        : null)
                .capturedAt(entity.getCapturedAt())
                .images(entity.getImages() != null 
                        ? entity.getImages().stream()
                                .map(PostureImageResponse::fromEntity)
                                .collect(java.util.stream.Collectors.toList())
                        : new ArrayList<>())
                .build();
    }
}

