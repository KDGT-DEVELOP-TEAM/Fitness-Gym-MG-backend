package com.example.fitnessgym_mg.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.fitnessgym_mg.entity.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
	
	/**
	 * 顧客IDに紐づくレッスン履歴を実施日時の降順で取得（履歴一覧表示用）
	 */
	List<Lesson> findByCustomerIdOrderByStartDateDesc(UUID customerId);
	
	// --- 1. レッスン一覧検索 (ページネーション対応) ---
	// 終了日時が指定時刻より前のレッスンを検索し、実施日時で降順ソートする。(全店舗対象)
	Page<Lesson> findPageByEndDateBefore(LocalDateTime endDate, Pageable pageable);

	// 店舗IDで絞り込み、かつ終了日時が指定時刻より前のレッスンを検索する。
	Page<Lesson> findPageByStoreIdAndEndDateBefore(UUID storeId, LocalDateTime endDate, Pageable pageable);

	// --- 2. グラフデータ集計 (既存を維持) ---
	// PostgreSQLの date_trunc を利用し、期間（週/月）別にレッスン回数を集計する。
	@Query(value = "SELECT date_trunc(:type, l.start_date) as period_start, COUNT(*) " +
			"FROM lessons l " +
			"WHERE l.end_date < :now AND " +
			" (:storeId IS NULL OR l.store_id = :storeId) " +
			"GROUP BY period_start " +
			"ORDER BY period_start DESC", nativeQuery = true)
	List<Object[]> countLessonsGroupedByPeriod(
			@Param("type") String type,
			@Param("now") LocalDateTime now,
			@Param("storeId") UUID storeId);

	// --- 3. 関連データカウント (既存を維持) ---
	/**
	 * 指定された顧客IDに紐づくレッスンレコードの件数を取得する。
	 */
	long countByCustomerId(UUID customerId);

	/**
	 * 指定されたユーザーIDに紐づくレッスンレコードの件数を取得する。
	 */
	long countByUserId(UUID userId);
}
