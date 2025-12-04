package com.example.fitnessgym_mg.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ホーム/ダッシュボード用レスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeResponse {
    
    // レッスン履歴一覧（最新の数件）
    private List<LessonResponse> recentLessons;
    
    // レッスン実施回数（統計情報）
    private Long totalLessonCount;
    
    // グラフデータ（Admin/Manager用）
    private LessonResponse.LessonChartData chartData;
    
    // 直近1週間のレッスン概要（Trainer用）
    private List<LessonResponse> upcomingLessons;
}

