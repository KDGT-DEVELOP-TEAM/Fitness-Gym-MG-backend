package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.Training;

@Repository
public interface TrainingRepository extends JpaRepository<Training, Training.TrainingId> {
    
    /**
     * レッスンIDで関連するトレーニングを順序順に取得
     */
    List<Training> findByIdLessonIdOrderByIdOrderNoAsc(UUID lessonId);
}

