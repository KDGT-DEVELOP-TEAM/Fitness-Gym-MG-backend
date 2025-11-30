package com.example.fitnessgym_mg.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.request.TrainingRequest;
import com.example.fitnessgym_mg.dto.response.TrainingResponse;
import com.example.fitnessgym_mg.entity.Training;
import com.example.fitnessgym_mg.repository.TrainingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingService {
    
    private final TrainingRepository trainingRepository;
    
    /**
     * トレーニングを作成して保存
     */
    @Transactional
    public List<Training> createTrainings(UUID lessonId, List<TrainingRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        
        List<Training> trainings = new ArrayList<>();
        for (TrainingRequest req : requests) {
            if (req.getName() != null && !req.getName().isBlank()) {
                Training.TrainingId trainingId = new Training.TrainingId(lessonId, req.getOrderNo());
                Training training = new Training();
                training.setId(trainingId);
                training.setName(req.getName());
                training.setReps(req.getReps());
                trainings.add(training);
            }
        }
        
        return trainingRepository.saveAll(trainings);
    }
    
    /**
     * レッスンIDでトレーニングを取得
     */
    public List<TrainingResponse> getTrainingsByLessonId(UUID lessonId) {
        List<Training> trainings = trainingRepository.findByIdLessonIdOrderByIdOrderNoAsc(lessonId);
        return trainings.stream()
            .map(t -> TrainingResponse.builder()
                .orderNo(t.getId().getOrderNo())
                .name(t.getName())
                .reps(t.getReps())
                .build())
            .collect(Collectors.toList());
    }
}

