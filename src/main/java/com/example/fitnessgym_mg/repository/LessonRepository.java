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
	 * CustomerをJOIN FETCHしないことで、@SQLRestrictionの適用を回避
	 * 
	 * <p>このメソッドは{@link com.example.fitnessgym_mg.dto.response.LessonResponse#fromEntity(Lesson)}で使用されることを前提としています。</p>
	 * <p><strong>設計上の前提</strong>: このメソッドで取得したLessonエンティティは、関連エンティティ（Store、Trainer）がJOIN FETCH済みです。</p>
	 * <p>CustomerはJOIN FETCHしないため、@SQLRestrictionの適用を回避します。Customer情報が必要な場合は、Service層でネイティブSQLクエリで別途取得します。</p>
	 * 
	 * @param customerId 顧客ID
	 * @return レッスンリスト（開始日時の降順）
	 */
	@Query("""
			SELECT DISTINCT l
			FROM Lesson l
			LEFT JOIN FETCH l.store
			LEFT JOIN FETCH l.trainer
			WHERE l.customer.id = :customerId
			ORDER BY l.startDate DESC
			""")
	List<Lesson> findByCustomerIdOrderByStartDateDesc(@Param("customerId") UUID customerId);

	/**
	 * 顧客IDに紐づくレッスン履歴をページネーション対応で取得（実施日時の降順）
	 * CustomerをJOIN FETCHしないことで、@SQLRestrictionの適用を回避
	 * 
	 * <p>このメソッドは{@link com.example.fitnessgym_mg.dto.response.LessonResponse#fromEntity(Lesson)}で使用されることを前提としています。</p>
	 * <p><strong>設計上の前提</strong>: このメソッドで取得したLessonエンティティは、関連エンティティ（Store、Trainer）がJOIN FETCH済みです。</p>
	 * <p>CustomerはJOIN FETCHしないため、@SQLRestrictionの適用を回避します。Customer情報が必要な場合は、Service層でネイティブSQLクエリで別途取得します。</p>
	 * 
	 * @param customerId 顧客ID
	 * @param pageable ページネーション情報
	 * @return レッスンページ（開始日時の降順）
	 */
	@Query(value = """
			SELECT DISTINCT l
			FROM Lesson l
			LEFT JOIN FETCH l.store
			LEFT JOIN FETCH l.trainer
			WHERE l.customer.id = :customerId
			ORDER BY l.startDate DESC
			""",
			countQuery = """
			SELECT COUNT(DISTINCT l)
			FROM Lesson l
			WHERE l.customer.id = :customerId
			""")
	Page<Lesson> findByCustomerIdOrderByStartDateDesc(@Param("customerId") UUID customerId, Pageable pageable);

	// --- 1. レッスン一覧検索 (ページネーション対応) ---

	/**
	 * 終了日時が指定時刻より前のレッスンを検索し、
	 * 実施日時（startDate）で降順ソートする（全店舗対象）
	 * 
	 * <p>このメソッドは{@link com.example.fitnessgym_mg.dto.response.LessonResponse#fromEntity(Lesson)}で使用されることを前提としています。</p>
	 * <p><strong>設計上の前提</strong>: このメソッドで取得したLessonエンティティは、関連エンティティ（Store、Trainer、Customer）がJOIN FETCH済みです。</p>
	 * 
	 * @param endDate 終了日時（この日時より前のレッスンを取得）
	 * @param pageable ページネーション情報
	 * @return レッスンページ
	 */
	@Query(value = """
			SELECT DISTINCT l
			FROM Lesson l
			LEFT JOIN FETCH l.store
			LEFT JOIN FETCH l.trainer
			LEFT JOIN FETCH l.customer
			WHERE l.endDate < :endDate
			ORDER BY l.startDate DESC
			""",
			countQuery = """
			SELECT COUNT(DISTINCT l)
			FROM Lesson l
			WHERE l.endDate < :endDate
			""")
	Page<Lesson> findByEndDateBefore(@Param("endDate") LocalDateTime endDate, Pageable pageable);

	/**
	 * 店舗IDで絞り込み、かつ終了日時が指定時刻より前のレッスンを検索する（一覧表示用）
	 * 
	 * <p>このメソッドは{@link com.example.fitnessgym_mg.dto.response.LessonResponse#fromEntity(Lesson)}で使用されることを前提としています。</p>
	 * <p><strong>設計上の前提</strong>: このメソッドで取得したLessonエンティティは、関連エンティティ（Store、Trainer、Customer）がJOIN FETCH済みです。</p>
	 * 
	 * @param storeId 店舗ID
	 * @param endDate 終了日時（この日時より前のレッスンを取得）
	 * @param pageable ページネーション情報
	 * @return レッスンページ
	 */
	@Query(value = """
			SELECT DISTINCT l
			FROM Lesson l
			LEFT JOIN FETCH l.store
			LEFT JOIN FETCH l.trainer
			LEFT JOIN FETCH l.customer
			WHERE l.store.id = :storeId
			  AND l.endDate < :endDate
			ORDER BY l.startDate DESC
			""",
			countQuery = """
			SELECT COUNT(DISTINCT l)
			FROM Lesson l
			WHERE l.store.id = :storeId
			  AND l.endDate < :endDate
			""")
	Page<Lesson> findByStoreIdAndEndDateBefore(@Param("storeId") UUID storeId, @Param("endDate") LocalDateTime endDate, Pageable pageable);

	// --- 2. グラフデータ集計 ---

	/**
	 * PostgreSQL の date_trunc を利用し、
	 * 期間（週/月など）別にレッスン回数を集計する（全店舗 or 店舗指定）
	 * 
	 * <p>型マッピングの問題を回避するため、Object[]で受け取り、Service層で手動マッピングする。</p>
	 * <p>結果: [periodStart (Timestamp), count (BigInteger)]</p>
	 */
	@Query(value = """
			SELECT date_trunc(:type, l.start_date)::timestamptz AS periodStart, 
			       COUNT(*)::bigint AS count
			FROM lessons l
			WHERE l.end_date < :now
			  AND (:storeId IS NULL OR l.store_id = :storeId)
			GROUP BY periodStart
			ORDER BY periodStart DESC
			""", nativeQuery = true)
	List<Object[]> countLessonsGroupedByPeriodRaw(
			@Param("type") String type,
			@Param("now") LocalDateTime now,
			@Param("storeId") UUID storeId);

	/**
	 * 顧客IDでグラフデータを集計（期間別レッスン回数）
	 * 
	 * <p>型マッピングの問題を回避するため、Object[]で受け取り、Service層で手動マッピングする。</p>
	 * <p>結果: [periodStart (Timestamp), count (BigInteger)]</p>
	 */
	@Query(value = """
			SELECT date_trunc(:type, l.start_date)::timestamptz AS periodStart, 
			       COUNT(*)::bigint AS count
			FROM lessons l
			WHERE l.end_date < :now
			  AND l.customer_id = :customerId
			GROUP BY periodStart
			ORDER BY periodStart DESC
			""", nativeQuery = true)
	List<Object[]> countLessonsGroupedByPeriodByCustomerIdRaw(
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
	 * トレーナー別の次回レッスン希望日程一覧を取得
	 * nextDateが設定されており、かつnextDateが未来の日時のレッスンのみ取得
	 * 
	 * <p>このメソッドは{@link com.example.fitnessgym_mg.dto.response.LessonResponse#fromEntity(Lesson)}で使用されることを前提としています。</p>
	 * <p><strong>設計上の前提</strong>: このメソッドで取得したLessonエンティティは、関連エンティティ（Store、Trainer、Customer、nextStore、nextUser）がJOIN FETCH済みである必要があります。</p>
	 * <p>JOIN FETCHが未使用の場合、{@link LessonResponse#fromEntity(Lesson)}内のnullチェックにより安全に処理されますが、パフォーマンス問題が発生する可能性があります。</p>
	 * 
	 * @param trainerId トレーナーID
	 * @param now 現在日時（nextDate > now の条件でフィルタ）
	 * @return 次回レッスン希望日程が設定されているレッスン一覧（nextDateの昇順）
	 */
	@Query("""
			SELECT DISTINCT l
			FROM Lesson l
			LEFT JOIN FETCH l.customer
			LEFT JOIN FETCH l.store
			LEFT JOIN FETCH l.trainer
			LEFT JOIN FETCH l.nextStore
			LEFT JOIN FETCH l.nextUser
			WHERE l.trainer.id = :trainerId
			  AND l.nextDate IS NOT NULL
			  AND l.nextDate > :now
			ORDER BY l.nextDate ASC
			""")
	List<Lesson> findNextLessonsByTrainerId(
			@Param("trainerId") UUID trainerId,
			@Param("now") LocalDateTime now);

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
	 * トレーナーIDで指定期間内のレッスンを取得する（ページネーション対応）
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
	org.springframework.data.domain.Page<Lesson> findUpcomingLessonsByTrainerIdBetween(
			@Param("trainerId") UUID trainerId,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate,
			org.springframework.data.domain.Pageable pageable);

	/**
	 * 次回トレーナーIDで指定期間内の次回レッスン希望を取得する（ページネーション対応）
	 * nextDateがfromDate以上かつtoDate未満のものをnextDate昇順で返す
	 * 
	 * <p>このメソッドは{@link com.example.fitnessgym_mg.dto.response.LessonResponse#fromEntity(Lesson)}で使用されることを前提としています。</p>
	 * <p><strong>設計上の前提</strong>: このメソッドで取得したLessonエンティティは、関連エンティティ（Store、Trainer、Customer、nextStore、nextUser）がJOIN FETCH済みである必要があります。</p>
	 * <p>JOIN FETCHが未使用の場合、{@link LessonResponse#fromEntity(Lesson)}内のnullチェックにより安全に処理されますが、パフォーマンス問題が発生する可能性があります。</p>
	 * 
	 * <p>注意: JOIN FETCHとページネーションを組み合わせる場合、カウントクエリを明示的に定義する必要があります。</p>
	 * 
	 * @param trainerId 次回トレーナーID（nextUser.id）
	 * @param fromDate 開始日時（nextDate >= fromDate の条件でフィルタ）
	 * @param toDate 終了日時（nextDate < toDate の条件でフィルタ）
	 * @param pageable ページネーション情報
	 * @return 次回レッスン希望日程が設定されているレッスンページ（nextDateの昇順）
	 */
	@Query(value = """
			SELECT DISTINCT l
			FROM Lesson l
			LEFT JOIN FETCH l.store
			LEFT JOIN FETCH l.trainer
			LEFT JOIN FETCH l.nextStore
			LEFT JOIN FETCH l.nextUser
			WHERE l.nextUser.id = :trainerId
			  AND l.nextDate IS NOT NULL
			  AND l.nextDate >= :fromDate
			  AND l.nextDate < :toDate
			ORDER BY l.nextDate ASC
			""",
			countQuery = """
			SELECT COUNT(DISTINCT l)
			FROM Lesson l
			WHERE l.nextUser.id = :trainerId
			  AND l.nextDate IS NOT NULL
			  AND l.nextDate >= :fromDate
			  AND l.nextDate < :toDate
			""")
	org.springframework.data.domain.Page<Lesson> findNextLessonsByNextTrainerIdBetween(
			@Param("trainerId") UUID trainerId,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate,
			org.springframework.data.domain.Pageable pageable);

	/**
	 * レッスンIDからCustomerのIDと名前を取得（@SQLRestrictionを回避するため、ネイティブSQLクエリを使用）
	 * 
	 * <p>ネイティブSQLクエリを使用することで、Hibernateの`@SQLRestriction`の影響を完全に回避できます。</p>
	 * <p>データベースに`deleted_at`カラムが存在しない場合でも、エラーが発生しません。</p>
	 * <p>重要: 論理削除条件（AND c.deleted_at IS NULL）を必ず含めること。</p>
	 * 
	 * @param lessonId レッスンID
	 * @return CustomerのIDと名前のペア（存在しない場合はnull）
	 */
	@Query(nativeQuery = true, value = "SELECT c.id, c.name FROM lessons l JOIN customers c ON l.customer_id = c.id WHERE l.id = :lessonId AND c.deleted_at IS NULL")
	java.util.Optional<Object[]> findCustomerIdAndNameByLessonId(@Param("lessonId") UUID lessonId);

	/**
	 * 複数のレッスンIDからCustomerのIDと名前を一括取得（@SQLRestrictionを回避するため、ネイティブSQLクエリを使用）
	 * 
	 * <p>ネイティブSQLクエリを使用することで、Hibernateの`@SQLRestriction`の影響を完全に回避できます。</p>
	 * <p>N+1問題を回避するため、複数のレッスンIDに対して一度のクエリでCustomer情報を取得します。</p>
	 * <p>重要: 論理削除条件（AND c.deleted_at IS NULL）を必ず含めること。</p>
	 * 
	 * @param lessonIds レッスンIDのリスト
	 * @return レッスンIDとCustomerのID、名前のマッピング（レッスンID -> [Customer ID, Customer Name]）
	 */
	@Query(nativeQuery = true, value = "SELECT l.id as lesson_id, c.id as customer_id, c.name as customer_name FROM lessons l JOIN customers c ON l.customer_id = c.id WHERE l.id IN :lessonIds AND c.deleted_at IS NULL")
	java.util.List<Object[]> findCustomerIdAndNameByLessonIds(@Param("lessonIds") java.util.List<UUID> lessonIds);

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
		      SELECT 1 FROM Lesson l2
		      WHERE l2.nextUser.id = :userId
		        AND l2.customer.id = c.id
		        AND l2.nextDate IS NOT NULL
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

	/**
	 * トレーナーが指定された顧客の次回レッスン希望日程の担当として設定されているか確認（存在確認専用クエリ）
	 * 
	 * <p>認可用クエリはexists系のみを使用し、「取得」と「可否判定」を混ぜない。</p>
	 * <p>トレーナーが顧客にアクセスできるかどうかを判断する際に使用される。</p>
	 * <p>user_customersテーブルにレコードがなくても、次回レッスン希望日程の担当として設定されていればアクセス可能とする。</p>
	 * 
	 * @param trainerId トレーナーID（nextUser.id）
	 * @param customerId 顧客ID
	 * @return 次回レッスン希望日程が存在する場合 true
	 */
	@Query("""
		SELECT CASE WHEN EXISTS (
			SELECT 1
			FROM Lesson l
			WHERE l.nextUser.id = :trainerId
			  AND l.customer.id = :customerId
			  AND l.nextDate IS NOT NULL
		) THEN true ELSE false END
		""")
	boolean existsNextLessonByTrainerIdAndCustomerId(
		@Param("trainerId") UUID trainerId,
		@Param("customerId") UUID customerId
	);
}