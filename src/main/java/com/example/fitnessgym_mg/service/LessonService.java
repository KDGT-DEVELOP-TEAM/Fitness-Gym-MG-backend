package com.example.fitnessgym_mg.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.example.fitnessgym_mg.dto.request.LessonRequest;
import com.example.fitnessgym_mg.dto.request.TrainingRequest;
import com.example.fitnessgym_mg.dto.response.LessonResponse;
import com.example.fitnessgym_mg.dto.response.PostureImageResponse;
import com.example.fitnessgym_mg.dto.response.TrainingResponse;
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.Lesson;
import com.example.fitnessgym_mg.entity.PostureGroup;
import com.example.fitnessgym_mg.entity.PostureImage;
import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.Training;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.repository.PostureGroupRepository;
import com.example.fitnessgym_mg.repository.StoreRepository;
import com.example.fitnessgym_mg.repository.TrainingRepository;
import com.example.fitnessgym_mg.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final TrainingRepository trainingRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final PostureGroupRepository postureGroupRepository;

    /**
     * 新規レッスンを作成
     * Lessonエンティティとトレーニング2種目を保存
     */
    @Transactional
    public Lesson createLesson(LessonRequest request) {
        // 関連エンティティを取得
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("顧客が見つかりません: " + request.getCustomerId()));
        
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new RuntimeException("店舗が見つかりません: " + request.getStoreId()));
        
        User trainer = userRepository.findById(request.getTrainerId())
                .orElseThrow(() -> new RuntimeException("トレーナーが見つかりません: " + request.getTrainerId()));
        
        Store nextStore = request.getNextStoreId() != null 
                ? storeRepository.findById(request.getNextStoreId()).orElse(null) 
                : null;
        
        User nextTrainer = request.getNextTrainerId() != null 
                ? userRepository.findById(request.getNextTrainerId()).orElse(null) 
                : null;

        // Lessonエンティティ作成
        Lesson lesson = Lesson.builder()
                .customer(customer)
                .store(store)
                .trainer(trainer)
                .condition(request.getCondition())
                .weight(request.getWeight())
                .meal(request.getMeal())
                .memo(request.getMemo())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .nextDate(request.getNextDate())
                .nextStore(nextStore)
                .nextTrainer(nextTrainer)
                .createdAt(OffsetDateTime.now())
                .build();
        
        // Lesson保存
        Lesson savedLesson = lessonRepository.save(lesson);
        
        // トレーニング保存
        if (request.getTrainings() != null && !request.getTrainings().isEmpty()) {
            for (TrainingRequest trainingRequest : request.getTrainings()) {
                Training training = Training.builder()
                        .id(Training.TrainingId.builder()
                                .lessonId(savedLesson.getId())
                                .orderNo(trainingRequest.getOrderNo())
                                .build())
                        .lesson(savedLesson)
                        .name(trainingRequest.getName())
                        .reps(trainingRequest.getReps())
                        .build();
                
                trainingRepository.save(training);
            }
        }
        
        return savedLesson;
    }

    /**
     * レッスン詳細を取得
     * Lesson、Training、PostureGroupを含む詳細データを取得してLessonResponseに変換
     */
    public LessonResponse getLessonDetail(UUID lessonId) {
        // Lessonを取得
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("レッスンが見つかりません: " + lessonId));
        
        // Trainingを取得
        List<Training> trainings = trainingRepository.findByIdLessonIdOrderByIdOrderNoAsc(lessonId);
        
        // PostureGroupを取得（あれば）
        List<PostureGroup> postureGroups = postureGroupRepository.findByLessonIdOrderByCapturedAtDesc(lessonId);
        
        // LessonResponseに変換
        return toLessonResponse(lesson, trainings, postureGroups);
    }

    /**
     * Lesson → LessonResponse変換
     */
    private LessonResponse toLessonResponse(Lesson lesson, List<Training> trainings, List<PostureGroup> postureGroups) {
        Customer customer = lesson.getCustomer();
        Store store = lesson.getStore();
        User trainer = lesson.getTrainer();
        Store nextStore = lesson.getNextStore();
        User nextTrainer = lesson.getNextTrainer();
        
        // Trainingリストを変換
        List<TrainingResponse> trainingResponses = trainings.stream()
                .map(t -> TrainingResponse.builder()
                        .orderNo(t.getId().getOrderNo())
                        .name(t.getName())
                        .reps(t.getReps())
                        .build())
                .collect(Collectors.toList());
        
        // PostureImageリストを変換（全PostureGroupから全画像を取得）
        List<PostureImageResponse> postureImageResponses = postureGroups.stream()
                .flatMap(pg -> pg.getImages().stream())
                .map(pi -> PostureImageResponse.builder()
                        .id(pi.getId())
                        .storageKey(pi.getStorageKey())
                        .position(pi.getPosition() != null ? pi.getPosition().getCode() : null)
                        .takenAt(pi.getTakenAt())
                        .consentPublication(pi.isConsentPublication())
                        .build())
                .collect(Collectors.toList());
        
        return LessonResponse.builder()
                .id(lesson.getId())
                .customerId(customer.getId())
                .customerName(customer.getName())
                .customerHeight(customer.getHeight())
                .storeId(store.getId())
                .storeName(store.getName())
                .trainerId(trainer.getId())
                .trainerName(trainer.getName())
                .condition(lesson.getCondition())
                .weight(lesson.getWeight())
                .meal(lesson.getMeal())
                .memo(lesson.getMemo())
                .startDate(lesson.getStartDate())
                .endDate(lesson.getEndDate())
                .nextDate(lesson.getNextDate())
                .nextStoreId(nextStore != null ? nextStore.getId() : null)
                .nextStoreName(nextStore != null ? nextStore.getName() : null)
                .nextTrainerId(nextTrainer != null ? nextTrainer.getId() : null)
                .nextTrainerName(nextTrainer != null ? nextTrainer.getName() : null)
                .createdAt(lesson.getCreatedAt())
                .trainings(trainingResponses)
                .postureImages(postureImageResponses)
                .build();
    }
}

