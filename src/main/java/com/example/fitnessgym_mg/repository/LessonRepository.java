package com.example.fitnessgym_mg.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.Lesson;
import com.example.fitnessgym_mg.repository.dto.PeriodCount;

/**
 * レッスンエンティティ用リポジトリ
 * レッスンの検索、履歴取得、グラフデータ集計などの機能を提供
 */
@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

	/**
	 * 顧客IDに紐づくレッスン履歴を実施日時の降順で取得（履歴一覧表示用）
	 * 
	 * <p>このメソッドは{@link com.example.fitnessgym_mg.dto.response.LessonResponse#fromEntity(Lesson)}で使用されることを前提としています。</p>
	 * <p><strong>設計上の前提</strong>: このメソッドで取得したLessonエンティティは、関連エンティティ（Store、Trainer、Customer）がJOIN FETCH済みである必要があります。</p>
	 * <p>JOIN FETCHが未使用の場合、{@link LessonResponse#fromEntity(Lesson)}内のnullチェックにより安全に処理されますが、パフォーマンス問題が発生する可能性があります。</p>
	 * 
	 * @param customerId 顧客ID
	 * @return レッスンリスト（開始日時の降順）
	 */
	List<Lesson> findByCustomerIdOrderByStartDateDesc(UUID customerId);

	/**
	 * 顧客IDに紐づくレッスン履歴をページネーション対応で取得（実施日時の降順）
	 * 
	 * <p>このメソッドは{@link com.example.fitnessgym_mg.dto.response.LessonResponse#fromEntity(Lesson)}で使用されることを前提としています。</p>
	 * <p><strong>設計上の前提</strong>: このメソッドで取得したLessonエンティティは、関連エンティティ（Store、Trainer、Customer）がJOIN FETCH済みである必要があります。</p>
	 * <p>JOIN FETCHが未使用の場合、{@link LessonResponse#fromEntity(Lesson)}内のnullチェックにより安全に処理されますが、パフォーマンス問題が発生する可能性があります。</p>
	 * 
	 * @param customerId 顧客ID
	 * @param pageable ページネーション情報
	 * @return レッスンページ（開始日時の降順）
	 */
	Page<Lesson> findByCustomerIdOrderByStartDateDesc(UUID customerId, Pageable pageable);

	// --- 1. レッスン一覧検索 (ページネーション対応) ---

	/**
	 * 終了日時が指定時刻より前のレッスンを検索し、
	 * 実施日時（startDate）で降順ソートする（全店舗対象）
	 * 
	 * <p>このメソッドは{@link com.example.fitnessgym_mg.dto.response.LessonResponse#fromEntity(Lesson)}で使用されることを前提としています。</p>
	 * <p><strong>設計上の前提</strong>: このメソッドで取得したLessonエンティティは、関連エンティティ（Store、Trainer、Customer）がJOIN FETCH済みである必要があります。</p>
	 * <p>JOIN FETCHが未使用の場合、{@link LessonResponse#fromEntity(Lesson)}内のnullチェックにより安全に処理されますが、パフォーマンス問題が発生する可能性があります。</p>
	 * 
	 * @param endDate 終了日時（この日時より前のレッスンを取得）
	 * @param pageable ページネーション情報
	 * @return レッスンページ
	 */
	Page<Lesson> findByEndDateBefore(LocalDateTime endDate, Pageable pageable);

	/**
	 * 店舗IDで絞り込み、かつ終了日時が指定時刻より前のレッスンを検索する（一覧表示用）
	 * 
	 * <p>このメソッドは{@link com.example.fitnessgym_mg.dto.response.LessonResponse#fromEntity(Lesson)}で使用されることを前提としています。</p>
	 * <p><strong>設計上の前提</strong>: このメソッドで取得したLessonエンティティは、関連エンティティ（Store、Trainer、Customer）がJOIN FETCH済みである必要があります。</p>
	 * <p>JOIN FETCHが未使用の場合、{@link LessonResponse#fromEntity(Lesson)}内のnullチェックにより安全に処理されますが、パフォーマンス問題が発生する可能性があります。</p>
	 * 
	 * @param storeId 店舗ID
	 * @param endDate 終了日時（この日時より前のレッスンを取得）
	 * @param pageable ページネーション情報
	 * @return レッスンページ
	 */
	Page<Lesson> findByStoreIdAndEndDateBefore(UUID storeId, LocalDateTime endDate, Pageable pageable);

	// --- 2. グラフデータ集計 ---

	/**
	 * PostgreSQL の date_trunc を利用し、
	 * 期間（週/月など）別にレッスン回数を集計する（全店舗 or 店舗指定）
	 * 
	 * <p>DTO Projectionを使用し、Repositoryの返却型への依存を排除。</p>
	 */
	@Query(value = """
			SELECT date_trunc(:type, l.start_date) AS periodStart, 
			       COUNT(*) AS count
			FROM lessons l
			WHERE l.end_date < :now
			  AND (:storeId IS NULL OR l.store_id = :storeId)
			GROUP BY periodStart
			ORDER BY periodStart DESC
			""", nativeQuery = true)
	List<PeriodCount> countLessonsGroupedByPeriod(
			@Param("type") String type,
			@Param("now") LocalDateTime now,
			@Param("storeId") UUID storeId);

	/**
	 * 顧客IDでグラフデータを集計（期間別レッスン回数）
	 * 
	 * <p>DTO Projectionを使用し、Repositoryの返却型への依存を排除。</p>
	 */
	@Query(value = """
			SELECT date_trunc(:type, l.start_date) AS periodStart, 
			       COUNT(*) AS count
			FROM lessons l
			WHERE l.end_date < :now
			  AND l.customer_id = :customerId
			GROUP BY periodStart
			ORDER BY periodStart DESC
			""", nativeQuery = true)
	List<PeriodCount> countLessonsGroupedByPeriodByCustomerId(
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
	@Query("SELECT COUNT(l) FROM Lesson l WHERE l.trainer.id = :trainerId")
	long countByTrainerId(@Param("trainerId") UUID trainerId);

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

	/**
	 * トレーナーIDで指定期間内のレッスンを取得する
	 * 開始日時がfromDate以上かつtoDate未満のものを開始日時昇順で返す
	 */
	@Query("""
			SELECT l
			FROM Lesson l
			WHERE l.trainer.id = :trainerId
			  AND l.startDate >= :fromDate
			  AND l.startDate < :toDate
			ORDER BY l.startDate ASC
			""")
	List<Lesson> findUpcomingLessonsByTrainerIdBetween(
			@Param("trainerId") UUID trainerId,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate);

	/**
	 * レッスンIDでレッスンを取得し、関連エンティティもJOIN FETCHで取得（N+1問題を回避）
	 * 
	 * <p>この用途（詳細画面）では現状の実装が妥当です。</p>
	 * <p>ただし、関連が増え続ける場合は、DTO Projectionに切り替える余地があります。</p>
	 * <p>パフォーマンスやメモリ使用量に懸念がある場合は、DTO Projectionへの移行を検討してください。</p>
	 */
	@Query("""
			SELECT DISTINCT l
			FROM Lesson l
			LEFT JOIN FETCH l.customer
			LEFT JOIN FETCH l.store
			LEFT JOIN FETCH l.trainer
			LEFT JOIN FETCH l.nextStore
			LEFT JOIN FETCH l.nextUser
			WHERE l.id = :lessonId
			""")
	java.util.Optional<Lesson> findByIdWithRelations(@Param("lessonId") UUID lessonId);

	/**
	 * レッスンIDから顧客IDを取得
	 * 認可チェック用の軽量なクエリ
	 * 
	 * @deprecated 認可用クエリはexists系のみを使用することを推奨。将来的に削除予定。
	 */
	@Deprecated
	@Query("SELECT l.customer.id FROM Lesson l WHERE l.id = :lessonId")
	java.util.Optional<UUID> findCustomerIdByLessonId(@Param("lessonId") UUID lessonId);

	/**
	 * ユーザーが指定されたレッスンにアクセス可能か確認（存在確認専用クエリ）
	 * 
	 * <p>認可用クエリはexists系のみを使用し、「取得」と「可否判定」を混ぜない。</p>
	 * <p>レッスンへのアクセス権限は顧客へのアクセス権限に依存するため、
	 * レッスンIDから顧客IDを取得し、顧客へのアクセス権限を確認する。</p>
	 * 
	 * @param userId ユーザーID
	 * @param lessonId レッスンID
	 * @return アクセス可能な場合 true
	 */
	@Query("""
		SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END
		FROM Lesson l
		JOIN l.customer c
		WHERE l.id = :lessonId
		  AND (
		    EXISTS (
		      SELECT 1 FROM User u
		      WHERE u.id = :userId
		        AND u.role = com.example.fitnessgym_mg.entity.enums.UserRole.ADMIN
		    )
		    OR EXISTS (
		      SELECT 1 FROM User u
		      JOIN u.stores ms
		      JOIN c.stores cs
		      WHERE u.id = :userId
		        AND ms.id = cs.id
		    )
		    OR EXISTS (
		      SELECT 1 FROM UserCustomer uc
		      WHERE uc.id.userId = :userId
		        AND uc.id.customerId = c.id
		    )
		  )
		""")
	boolean existsAccessibleLesson(
		@Param("userId") UUID userId,
		@Param("lessonId") UUID lessonId
	);

	/**
	 * 顧客IDで最新のレッスンを1件のみ取得（体重取得用）
	 */
	@Query("""
			SELECT l
			FROM Lesson l
			WHERE l.customer.id = :customerId
			  AND l.weight IS NOT NULL
			ORDER BY l.startDate DESC
			""")
	java.util.List<Lesson> findLatestLessonsWithWeightByCustomerId(@Param("customerId") UUID customerId, org.springframework.data.domain.Pageable pageable);
}