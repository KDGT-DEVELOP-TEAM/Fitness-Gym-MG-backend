package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.PostureGroup;

/**
 * DB posture_groupsテーブルへのアクセスリポジトリ
 */
@Repository
public interface PostureGroupRepository extends JpaRepository<PostureGroup, UUID> {

    /**
     * 顧客IDに紐づく姿勢画像グループ一覧を取得
     * 
     * <p>@EntityGraphを使用してN+1問題を回避し、関連データを1回のクエリで取得します。</p>
     * <p>- customer, lesson, images: 必須関連をJOIN FETCHで取得</p>
     * <p>ソート順: レッスン開始日の降順 → 撮影日時の降順（最新順）</p>
     * 
     * <p>注意: JPA仕様上、FETCH JOIN + DISTINCT + ORDER BYの順序は保証されないため、
     * @EntityGraphを使用することで、順序保証の問題を回避しています。</p>
     */
    @EntityGraph(attributePaths = {"customer", "lesson", "images"})
    @Query("""
            SELECT pg
            FROM PostureGroup pg
            WHERE pg.customer.id = :customerId
            ORDER BY pg.lesson.startDate DESC, pg.capturedAt DESC
            """)
    List<PostureGroup> findAllWithImagesByCustomerId(@Param("customerId") UUID customerId);
    
    /**
     * レッスンIDに紐づく姿勢画像グループ一覧を取得（撮影日時の降順）
     */
    List<PostureGroup> findByLessonIdOrderByCapturedAtDesc(UUID lessonId);
    
    /**
     * 顧客IDに紐づく姿勢画像グループ一覧を取得（撮影日時の降順）
     * 注: findAllWithImagesByCustomerId()と機能が重複するため、用途に応じて使い分け
     * - findAllWithImagesByCustomerId(): JOIN FETCHで最適化（関連データも取得）
     * - findByCustomerIdOrderByCapturedAtDesc(): シンプルな検索（関連データは遅延読み込み）
     */
    List<PostureGroup> findByCustomerIdOrderByCapturedAtDesc(UUID customerId);
    
    /**
     * レッスンIDに紐づく姿勢画像グループが存在するか確認
     * 冪等性チェック用
     */
    boolean existsByLessonId(UUID lessonId);
    
    /**
     * PostureGroupIDからLessonIDを取得
     * 認可チェック用の軽量なクエリ
     * 
     * @deprecated 認可用クエリはexists系のみを使用することを推奨。将来的に削除予定。
     * 
     * @param postureGroupId PostureGroupID
     * @return LessonID（存在する場合）
     */
    @Deprecated
    @Query("SELECT pg.lesson.id FROM PostureGroup pg WHERE pg.id = :postureGroupId")
    Optional<UUID> findLessonIdByPostureGroupId(@Param("postureGroupId") UUID postureGroupId);
}

