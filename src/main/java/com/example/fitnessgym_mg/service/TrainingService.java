package com.example.fitnessgym_mg.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.request.TrainingRequest;
import com.example.fitnessgym_mg.dto.response.TrainingResponse;
import com.example.fitnessgym_mg.entity.Lesson;
import com.example.fitnessgym_mg.entity.Training;
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.repository.TrainingRepository;

import lombok.RequiredArgsConstructor;

/**
 * トレーニング管理サービス
 * トレーニング種目の作成（レッスンに紐づけて保存）、レッスンに紐づくトレーニング種目の取得を担当
 */
@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingRepository trainingRepository;
    private final LessonRepository lessonRepository;

    /**
     * トレーニング種目を作成
     * 
     * 処理の流れ：
     * 1. レッスンIDでレッスンエンティティを取得
     * 2. 各TrainingRequest（DTO）からTrainingエンティティを作成
     * 3. 各Trainingエンティティをデータベースに保存
     */
    @Transactional
    public void createTrainings(UUID lessonId, List<TrainingRequest> trainingRequests) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("レッスンが見つかりません: " + lessonId));
        
        for (TrainingRequest request : trainingRequests) {
            Training training = Training.builder()
                    .id(Training.TrainingId.builder()
                            .lessonId(lessonId)
                            .orderNo(request.getOrderNo())
                            .build())
                    .lesson(lesson)
                    .name(request.getName())
                    .reps(request.getReps())
                    .build();
            
            trainingRepository.save(training);
        }
    }

    /**
     * レッスンIDでトレーニング一覧を取得
     * 
     * 処理の流れ：
     * 1. レッスンIDでトレーニング種目を取得（順序番号の小さい順）
     * 2. 各TrainingエンティティをTrainingResponse（DTO）に変換
     * 3. TrainingResponseのリストを返す
     */
    public List<TrainingResponse> getTrainingsByLessonId(UUID lessonId) {
        return trainingRepository.findByIdLessonIdOrderByIdOrderNoAsc(lessonId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * TrainingエンティティをTrainingResponse（DTO）に変換
     */
    private TrainingResponse toResponse(Training training) {
        return TrainingResponse.builder()
                .orderNo(training.getId().getOrderNo())  // 順序番号（複合主キーの一部）
                .name(training.getName())  // トレーニング名称
                .reps(training.getReps())  // 回数
                .build();
    }
}

