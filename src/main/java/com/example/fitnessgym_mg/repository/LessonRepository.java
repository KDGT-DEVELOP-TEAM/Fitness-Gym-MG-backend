package com.example.fitnessgym_mg.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.fitnessgym_mg.entity.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
	// 終了日時が指定時刻より前のレッスンを検索し、実施日時で降順ソートする。
	// @param endDate 終了日時の比較基準
	List<Lesson> findByEndDateBeforeOrderByStartDateDesc(LocalDateTime endDate);

	// 店舗IDで絞り込み、かつ終了日時が指定時刻より前のレッスンを検索する。
	// @param storeId 絞り込み対象の店舗ID
	// @param endDate 終了日時の比較基準
	List<Lesson> findByStoreIdAndEndDateBeforeOrderByStartDateDesc(UUID storeId, LocalDateTime endDate);

	// PostgreSQLの date_trunc を利用し、期間（週/月）別にレッスン回数を集計する。
	// 結果は、[期間の開始日時 (Timestamp), 回数 (Long)] のリストとして返される。
	@Query(value = "SELECT date_trunc(:type, l.start_date) as period_start, COUNT(l) " +
			"FROM lesson l " +
			"WHERE l.end_date < :now AND " +
			"      (:storeId IS NULL OR l.store_id = :storeId) " +
			"GROUP BY period_start " +
			"ORDER BY period_start DESC", nativeQuery = true)
	List<Object[]> countLessonsGroupedByPeriod(
			@Param("type") String type,
			@Param("now") LocalDateTime now,
			@Param("storeId") UUID storeId);
}
