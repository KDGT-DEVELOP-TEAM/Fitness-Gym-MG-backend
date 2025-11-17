package com.example.fitnessgym_mg.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.response.LessonResponse;
import com.example.fitnessgym_mg.dto.response.LessonResponse.LessonChartData;
import com.example.fitnessgym_mg.entity.Lesson;
import com.example.fitnessgym_mg.repository.LessonRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {

	private final LessonRepository lessonRepository;

	// --- レッスン一覧の検索と絞り込み ---
	public List<LessonResponse> searchLessons(String storeId, String keyword) {

		List<Lesson> lessons;
		LocalDateTime now = LocalDateTime.now();

		// 1. 絞り込み (店舗ID + 終了日時)
		if (storeId != null && !storeId.isEmpty()) {
			// 店舗IDで絞り込み、終了日時が現在時刻より前のものを取得 (DBでソート済み)
			UUID storeUuid = UUID.fromString(storeId);
			// findByStoreIdAndEndDateBeforeOrderByStartDateDesc を使用
			lessons = lessonRepository.findByStoreIdAndEndDateBeforeOrderByStartDateDesc(storeUuid, now);
		} else {
			// 店舗絞り込みなし: 終了日時が現在時刻より前のものを取得 (DBでソート済み)
			lessons = lessonRepository.findByEndDateBeforeOrderByStartDateDesc(now);
		}

		return lessons.stream()
				.map(LessonResponse::fromEntity)
				.toList();
	}

	// --- レッスン回数グラフデータの作成 ---
	public LessonResponse.LessonChartData getLessonChartData(String storeId, String type) {

		LocalDateTime now = LocalDateTime.now();
		UUID storeUuid = (storeId != null && !storeId.isEmpty()) ? UUID.fromString(storeId) : null;

		// 1. 期間タイプの決定とJPQL呼び出し
		String intervalType;
		if ("week".equals(type)) {
			// PostgreSQLの date_trunc('week', ...) を使用
			intervalType = "week";
		} else {
			// PostgreSQLの date_trunc('month', ...) を使用
			intervalType = "month";
			type = "month"; // typeの値を念のため統一
		}

		// DBから集計結果を取得 [0: 期間開始日時, 1: 回数]
		List<Object[]> rawChartData = lessonRepository.countLessonsGroupedByPeriod(
				intervalType, now, storeUuid);

		// 2. 結果の整形 (LessonChartDataの生成)
		List<Map<String, Object>> series = new ArrayList<>();
		int maxCount = 0;

		// DBから降順（新しい順）で取得しているため、後で reverse は不要だが、
		// グラフ描画要件（右が最新）と合わせるため、処理順序は保持します。
		for (Object[] row : rawChartData) {
			// PostgreSQLはTIMESTAMP型を返すため、LocalDateTimeに変換
			java.sql.Timestamp periodTimestamp = (java.sql.Timestamp) row[0];
			LocalDateTime periodStartAt = periodTimestamp.toInstant()
					.atZone(ZoneId.systemDefault()) // タイムゾーンを考慮
					.toLocalDateTime();

			long count = ((Number) row[1]).longValue();

			// ラベルの生成
			String label;
			if ("week".equals(type)) {
				// PostgreSQLの date_trunc('week') は通常、月曜日を返す（ただし設定依存）。
				// Java側でラベル整形を行う
				LocalDate startDate = periodStartAt.toLocalDate();
				LocalDate endDate = startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
				label = startDate.getMonthValue() + "/" + startDate.getDayOfMonth() +
						" - " + endDate.getMonthValue() + "/" + endDate.getDayOfMonth();
			} else {
				// 月別
				label = periodStartAt.getYear() + "/" + periodStartAt.getMonthValue();
			}

			// maxCountの更新
			int currentCount = (int) count;
			if (currentCount > maxCount) {
				maxCount = currentCount;
			}

			Map<String, Object> dataPoint = new HashMap<>();
			dataPoint.put("label", label);
			dataPoint.put("count", count);
			dataPoint.put("countLabel", count + "回");
			series.add(dataPoint);
		}

		// グラフの要件に従い、「右が最新」にするため、リストを逆順にする (DBで降順取得 -> Javaで逆順追加)
		// DBで降順取得（最新が先頭）しているため、Java Stream時代のロジックを踏襲し、
		// 最終的な表示順（右が最新）にするために、リストを反転させます。
		java.util.Collections.reverse(series);

		LessonChartData chartData = new LessonChartData();
		chartData.setSeries(series);
		chartData.setMaxCount(maxCount);
		chartData.setType(type);
		return chartData;
	}
}