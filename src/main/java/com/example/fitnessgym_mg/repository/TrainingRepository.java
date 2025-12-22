package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
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
}

