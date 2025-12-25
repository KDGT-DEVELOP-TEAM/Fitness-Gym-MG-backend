package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.UUID;

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
     * JOIN FETCH: N+1問題を回避するため関連データを1回のクエリでDBから取得
     * - customer, lesson: 必須関連なのでJOIN FETCH
     * - images: 任意関連なのでLEFT JOIN FETCH（画像がない場合もグループを取得）
     * ソート順: レッスン開始日の降順 → 撮影日時の降順（最新順）
     */
    @Query("""
            SELECT DISTINCT pg
            FROM PostureGroup pg
            JOIN FETCH pg.customer c
            LEFT JOIN FETCH pg.images imgs
            JOIN FETCH pg.lesson l
            WHERE c.id = :customerId
            ORDER BY l.startDate DESC, pg.capturedAt DESC
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
     * @param postureGroupId PostureGroupID
     * @return LessonID（存在する場合）
     */
    @Query("SELECT pg.lesson.id FROM PostureGroup pg WHERE pg.id = :postureGroupId")
    Optional<UUID> findLessonIdByPostureGroupId(@Param("postureGroupId") UUID postureGroupId);
}

