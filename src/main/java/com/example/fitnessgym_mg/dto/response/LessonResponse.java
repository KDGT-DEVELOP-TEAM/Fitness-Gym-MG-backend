package com.example.fitnessgym_mg.dto.response;

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
	private Double weight;
	private Double bmi; // 計算値
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
	public static Double calculateBmi(Double weight, Double height) {
		if (weight == null || height == null || height == 0) {
			return null;
		}
		double heightInMeters = height / 100.0;
		return Math.round((weight / (heightInMeters * heightInMeters)) * 100.0) / 100.0;
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