package com.example.fitnessgym_mg.dto.response;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureGroupResponse {

    private UUID id;
    private UUID lessonId;
    
    // lessonStartDate: Lessonエンティティのstart_dateフィールドから取得
    // 注: マージ後にLessonエンティティの実装を確認すること
    private OffsetDateTime lessonStartDate;
    
    private OffsetDateTime capturedAt;
    
    @Builder.Default
    private List<PostureImageResponse> images = new ArrayList<>();
}

