package com.example.fitnessgym_mg.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.Lesson;

import lombok.Data;

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
	private String customerName;

	// Lesson エンティティから DTO に変換する
	public static LessonResponse fromEntity(Lesson lesson) {
		LessonResponse r = new LessonResponse();
		r.setId(lesson.getId());
		r.setStartDate(lesson.getStartDate());
		r.setEndDate(lesson.getEndDate());

		// 関連エンティティから名前を取得（※関連がロードされている前提）
		r.setStoreName(lesson.getStore().getName());
		r.setTrainerName(lesson.getTrainer().getName());
		r.setCustomerName(lesson.getCustomer().getName());

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
		private int maxCount;
		private String type;
	}

}