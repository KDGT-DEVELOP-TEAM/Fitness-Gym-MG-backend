package com.example.fitnessgym_mg.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ホーム/ダッシュボード用レスポンスDTO
 * 
 * <p>ロール別の使用フィールド:</p>
 * <ul>
 *   <li><strong>Admin/Manager用</strong>:
 *     <ul>
 *       <li>{@link #recentLessons}: レッスン履歴一覧（最新の数件）</li>
 *       <li>{@link #totalLessonCount}: レッスン実施回数（統計情報）</li>
 *       <li>{@link #chartData}: グラフデータ</li>
 *       <li>{@link #upcomingLessons}: null（使用しない）</li>
 *     </ul>
 *   </li>
 *   <li><strong>Trainer用</strong>:
 *     <ul>
 *       <li>{@link #upcomingLessons}: 直近1週間のレッスン概要</li>
 *       <li>{@link #recentLessons}: null（使用しない）</li>
 *       <li>{@link #totalLessonCount}: 0（統計情報は表示しない）</li>
 *       <li>{@link #chartData}: null（使用しない）</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <p><strong>依存関係</strong>: このDTOは{@link LessonResponse}とその内部クラス{@link LessonResponse.LessonChartData}に依存しています。</p>
 * <p>将来的に{@link LessonResponse}の構造が変更される可能性がある場合は、軽量View DTOの導入を検討してください。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeResponse {
    
    /**
     * レッスン履歴一覧（最新の数件）
     * 
     * <p>Admin/Manager用: レッスン履歴を表示するために使用</p>
     * <p>Trainer用: null（使用しない）</p>
     */
    private List<LessonResponse> recentLessons;
    
    /**
     * レッスン実施回数（統計情報）
     * 
     * <p>常に0以上の値を返します。nullは返しません。</p>
     * <p>ロール別の使用状況:</p>
     * <ul>
     *   <li>Admin/Manager: レッスン履歴の総件数を設定</li>
     *   <li>Trainer: 0を設定（統計情報は表示しない）</li>
     * </ul>
     */
    private long totalLessonCount;
    
    /**
     * グラフデータ（Admin/Manager用）
     * 
     * <p>Admin/Manager用: レッスン実施状況のグラフ表示に使用</p>
     * <p>Trainer用: null（使用しない）</p>
     */
    private LessonResponse.LessonChartData chartData;
    
    /**
     * 直近1週間のレッスン概要（Trainer用）
     * 
     * <p>Trainer用: 当日・直近1週間以内の予約状況/レッスン概要を表示するために使用</p>
     * <p>Admin/Manager用: null（使用しない）</p>
     */
    private List<LessonResponse> upcomingLessons;
}

