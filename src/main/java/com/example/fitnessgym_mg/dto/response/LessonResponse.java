package com.example.fitnessgym_mg.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.Lesson;

import lombok.Data;

/**
 * レッスンレスポンスDTO
 * レッスン情報をAPIレスポンスとして返す際に使用
 * 関連エンティティ（店舗、トレーナー、顧客）の名前も含む
 */
@Data
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

	// Lesson エンティティから DTO に変換する
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

	// BMI計算メソッド
	public static BigDecimal calculateBmi(BigDecimal weight, BigDecimal height) {
		if (weight == null || height == null || height.compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}
		BigDecimal heightInMeters = height.divide(
			new BigDecimal(com.example.fitnessgym_mg.config.ApplicationConstants.HEIGHT_CONVERSION_FACTOR), 
			2, RoundingMode.HALF_UP);
		BigDecimal bmi = weight.divide(heightInMeters.multiply(heightInMeters), 2, RoundingMode.HALF_UP);
		return bmi;
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
		private int maxCount;
		private String type;
	}

}