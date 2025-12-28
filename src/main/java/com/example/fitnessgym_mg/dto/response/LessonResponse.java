package com.example.fitnessgym_mg.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.Lesson;

import lombok.Data;
import lombok.ToString;

/**
 * レッスンレスポンスDTO
 * レッスン情報をAPIレスポンスとして返す際に使用
 * 関連エンティティ（店舗、トレーナー、顧客）の名前も含む
 * 
 * <p>セキュリティ: 個人情報（memo、meal）とサイズの大きいデータ（postureImages、trainings）はログ出力から除外します。</p>
 */
@Data
@ToString(exclude = {"memo", "meal", "postureImages", "trainings"}) // セキュリティ: 個人情報とサイズの大きいデータをログに出力しない
public class LessonResponse {

	private UUID id;

	// 実施日時
	private LocalDateTime startDate;
	private LocalDateTime endDate;

	// 実施店舗
	private String storeName;

	// 担当トレーナー
	private String trainerName;

	// 顧客
	private UUID customerId;
	private String customerName;

	// 詳細表示用フィールド
	private String condition;
	private BigDecimal weight;
	private BigDecimal bmi; // 計算値
	private String meal;
	private String memo;
	private LocalDateTime nextDate;
	private String nextStoreName;
	private String nextTrainerName;
	
	private List<TrainingResponse> trainings;
	private List<PostureImageResponse> postureImages;

	/**
	 * Lesson エンティティから DTO に変換する（一覧表示用）
	 * 
	 * <p>このメソッドは一覧表示用の基本フィールドのみを設定します。</p>
	 * <p>設定されるフィールド:</p>
	 * <ul>
	 *   <li>id, startDate, endDate</li>
	 *   <li>storeName, trainerName</li>
	 *   <li>customerId, customerName</li>
	 * </ul>
	 * <p>設定されないフィールド（詳細表示用）:</p>
	 * <ul>
	 *   <li>condition, weight, bmi, meal, memo</li>
	 *   <li>nextDate, nextStoreName, nextTrainerName</li>
	 *   <li>trainings, postureImages</li>
	 * </ul>
	 * <p>詳細フィールドが必要な場合は、{@link com.example.fitnessgym_mg.service.LessonService#getLessonDetail(UUID)}を使用してください。</p>
	 * 
	 * @param lesson Lessonエンティティ
	 * @return LessonResponse（基本フィールドのみ設定）
	 */
	public static LessonResponse fromEntity(Lesson lesson) {
		if (lesson == null) {
			return null;
		}
		
		LessonResponse r = new LessonResponse();
		r.setId(lesson.getId());
		r.setStartDate(lesson.getStartDate());
		r.setEndDate(lesson.getEndDate());

		// 関連エンティティのnullチェック
		if (lesson.getStore() != null) {
			r.setStoreName(lesson.getStore().getName());
		}
		if (lesson.getTrainer() != null) {
			r.setTrainerName(lesson.getTrainer().getName());
		}
		if (lesson.getCustomer() != null) {
			r.setCustomerId(lesson.getCustomer().getId());
			r.setCustomerName(lesson.getCustomer().getName());
		}

		return r;
	}


	// グラフデータを格納するための内部クラス
	@Data
	public static class ChartSeries {
		// PostgreSQLのdate_truncから取得される開始期間
		private String period;
		private long count; // long count;
	}

	@Data
	public static class LessonChartData {
		private List<ChartSeries> series;
		private long maxCount;
		private String type;
	}

}