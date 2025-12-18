package com.example.fitnessgym_mg.repository;

import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.fitnessgym_mg.entity.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

	/**
	 * 顧客IDに紐づくレッスン履歴を実施日時の降順で取得（履歴一覧表示用）
	 */
	List<Lesson> findByCustomerIdOrderByStartDateDesc(UUID customerId);

	/**
	 * 顧客IDに紐づくレッスン履歴をページネーション対応で取得（実施日時の降順）
	 */
	Page<Lesson> findByCustomerIdOrderByStartDateDesc(UUID customerId, Pageable pageable);

	// --- 1. レッスン一覧検索 (ページネーション対応) ---

	/**
	 * 終了日時が指定時刻より前のレッスンを検索し、
	 * 実施日時（startDate）で降順ソートする（全店舗対象）
	 */
	Page<Lesson> findByEndDateBefore(LocalDateTime endDate, Pageable pageable);

	/**
	 * 店舗IDで絞り込み、かつ終了日時が指定時刻より前のレッスンを検索する
	 */
	Page<Lesson> findByStoreIdAndEndDateBefore(UUID storeId, LocalDateTime endDate, Pageable pageable);

	// --- 2. グラフデータ集計 ---

	/**
	 * PostgreSQL の date_trunc を利用し、
	 * 期間（週/月など）別にレッスン回数を集計する（全店舗 or 店舗指定）
	 */
	@Query(value = """
			SELECT date_trunc(:type, l.start_date) AS period_start, COUNT(*)
			FROM lessons l
			WHERE l.end_date < :now
			  AND (:storeId IS NULL OR l.store_id = :storeId)
			GROUP BY period_start
			ORDER BY period_start DESC
			""", nativeQuery = true)
	List<Object[]> countLessonsGroupedByPeriod(
			@Param("type") String type,
			@Param("now") LocalDateTime now,
			@Param("storeId") UUID storeId);

	/**
	 * 顧客IDでグラフデータを集計（期間別レッスン回数）
	 */
	@Query(value = """
			SELECT date_trunc(:type, l.start_date) AS period_start, COUNT(*)
			FROM lessons l
			WHERE l.end_date < :now
			  AND l.customer_id = :customerId
			GROUP BY period_start
			ORDER BY period_start DESC
			""", nativeQuery = true)
	List<Object[]> countLessonsGroupedByPeriodByCustomerId(
			@Param("type") String type,
			@Param("now") LocalDateTime now,
			@Param("customerId") UUID customerId);

	// --- 3. 関連データカウント ---

	/**
	 * 指定された顧客IDに紐づくレッスンレコードの件数を取得する
	 */
	long countByCustomerId(UUID customerId);

	/**
	 * 指定されたトレーナーID（ユーザーID）に紐づく
	 * レッスンレコードの件数を取得する
	 */
	long countByTrainerId(UUID trainerId);

	/**
	 * トレーナーIDで直近のレッスンを取得する
	 * 開始日時が指定日時以降のものを開始日時昇順で返す
	 */
	@Query("""
			SELECT l
			FROM Lesson l
			WHERE l.trainer.id = :trainerId
			  AND l.startDate >= :fromDate
			ORDER BY l.startDate ASC
			""")
	List<Lesson> findUpcomingLessonsByTrainerId(
			@Param("trainerId") UUID trainerId,
			@Param("fromDate") LocalDateTime fromDate);
}