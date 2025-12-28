package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.Training;

/**
 * トレーニングエンティティ用リポジトリ
 * トレーニングの検索、レッスン別取得などの機能を提供
 * 複合主キー（TrainingId）を使用
 */
@Repository
public interface TrainingRepository extends JpaRepository<Training, Training.TrainingId> {
    
    /**
     * レッスンIDで関連するトレーニングを順序順に取得
     * 
     * @param lessonId レッスンID
     * @return トレーニングのリスト（順序順）
     */
    List<Training> findByIdLessonIdOrderByIdOrderNoAsc(UUID lessonId);
    
    /**
     * レッスンIDで関連するトレーニングを一括削除
     * 
     * <p>パフォーマンス最適化: 全件SELECT → DELETEの代わりに、直接DELETEクエリを実行します。</p>
     * 
     * <p>設計方針:</p>
     * <ul>
     *   <li>Trainingは派生データ（レッスンの一部）なので物理削除を前提としています。</li>
     *   <li>Customer、Lessonは論理削除前提ですが、Trainingは物理削除です。</li>
     *   <li>これは設計上の不整合ではなく、Trainingがレッスンの一部として扱われるためです。</li>
     *   <li>レッスン更新時は、既存のTrainingを削除して新規作成する前提です。</li>
     * </ul>
     * 
     * <p>注意: 同一トランザクション内でこのメソッド実行後にTrainingを参照する場合は、
     * @Modifying(clearAutomatically = true)によりPersistence Contextが自動的にクリアされます。</p>
     * 
     * @param lessonId レッスンID
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Training t WHERE t.id.lessonId = :lessonId")
    void deleteByLessonId(@Param("lessonId") UUID lessonId);
}

