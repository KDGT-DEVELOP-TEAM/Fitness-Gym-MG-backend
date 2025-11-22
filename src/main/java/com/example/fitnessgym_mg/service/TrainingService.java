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

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingRepository trainingRepository;
    private final LessonRepository lessonRepository;

    /**
     * トレーニング2種目を作成
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
     */
    public List<TrainingResponse> getTrainingsByLessonId(UUID lessonId) {
        return trainingRepository.findByIdLessonIdOrderByIdOrderNoAsc(lessonId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TrainingResponse toResponse(Training training) {
        return TrainingResponse.builder()
                .orderNo(training.getId().getOrderNo())
                .name(training.getName())
                .reps(training.getReps())
                .build();
    }
}

