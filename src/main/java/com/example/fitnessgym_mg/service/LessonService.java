package com.example.fitnessgym_mg.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.response.LessonResponse;
import com.example.fitnessgym_mg.dto.response.LessonResponse.ChartSeries;
import com.example.fitnessgym_mg.dto.response.LessonResponse.LessonChartData;
import com.example.fitnessgym_mg.entity.Lesson;
import com.example.fitnessgym_mg.repository.LessonRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {

	private final LessonRepository lessonRepository;

	// --- レッスン一覧の検索と絞り込み (Pageable対応に修正) ---
	// ★ Pageable を引数に追加し、戻り値を Page に変更 ★
	public Page<LessonResponse> searchLessons(UUID storeId, String keyword, Pageable pageable) {

		Page<Lesson> lessonPage;
		LocalDateTime now = LocalDateTime.now();

		// ソートは Repository メソッド名で定義されているため、Pageableにはサイズとページ番号のみを渡す
		// findByStoreIdAndEndDateBefore... (ソート済み) を使用するため、Pageableはソート情報なしでOK

		// 1. 絞り込み (店舗ID + 終了日時)
		if (storeId != null) {
			// 店舗IDで絞り込み
			// Repositoryに findPageByStoreIdAndEndDateBeforeOrderByStartDateDesc(UUID, LocalDateTime, Pageable) が必要
			lessonPage = lessonRepository.findPageByStoreIdAndEndDateBefore(storeId, now, pageable);
		} else {
			// 店舗絞り込みなし
			// Repositoryに findPageByEndDateBeforeOrderByStartDateDesc(LocalDateTime, Pageable) が必要
			lessonPage = lessonRepository.findPageByEndDateBefore(now, pageable);
		}

		// 2. マッピング
		return lessonPage.map(LessonResponse::fromEntity);
	}

	// --- レッスン回数グラフデータの作成 ---
	public LessonChartData getLessonChartData(UUID storeId, String type) {

		LocalDateTime now = LocalDateTime.now();
		UUID storeUuid = storeId;

		// 1. 期間タイプの決定とJPQL呼び出し
		String intervalType;
		if ("week".equals(type)) {
			intervalType = "week";
		} else {
			intervalType = "month";
			type = "month";
		}

		// DBから集計結果を取得 [0: 期間開始日時, 1: 回数]
		List<Object[]> rawChartData = lessonRepository.countLessonsGroupedByPeriod(
				intervalType, now, storeUuid);

		// 2. 結果の整形 (LessonChartDataの生成)
		List<ChartSeries> series = new ArrayList<>();
		int maxCount = 0;

		for (Object[] row : rawChartData) {
			// PostgreSQLはTIMESTAMP型を返すため、LocalDateTimeに変換
			java.sql.Timestamp periodTimestamp = (java.sql.Timestamp) row[0];
			LocalDateTime periodStartAt = periodTimestamp.toInstant()
					.atZone(ZoneId.systemDefault())
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

			ChartSeries chartSeries = new ChartSeries();
			chartSeries.setPeriod(label); // ラベルを periodStart (期間の表示名) として使用
			chartSeries.setCount(count);
			series.add(chartSeries);
		}

		// グラフの要件に従い、「右が最新」にするため、リストを逆順にする
		java.util.Collections.reverse(series);

		LessonChartData chartData = new LessonChartData();
		chartData.setSeries(series);
		chartData.setMaxCount(maxCount);
		chartData.setType(type);
		return chartData;
	}
}