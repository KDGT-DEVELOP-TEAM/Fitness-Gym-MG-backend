package com.example.fitnessgym_mg.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.request.TrainingRequest;
import com.example.fitnessgym_mg.dto.response.TrainingResponse;
import com.example.fitnessgym_mg.entity.Training;
import com.example.fitnessgym_mg.exception.ConflictException;
import com.example.fitnessgym_mg.exception.InvalidRequestException;
import com.example.fitnessgym_mg.repository.TrainingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingService {
    
    private final TrainingRepository trainingRepository;
    
    /**
     * トレーニングを作成して保存
     * 
     * <p>バリデーション方針:</p>
     * <ul>
     *   <li>1件でも不正なrequestが含まれている場合、全体をエラーとする（部分成功を防止）</li>
     *   <li>同一lessonId内でorderNoが重複している場合、エラーとする</li>
     *   <li>@Transactionalにより、エラー発生時は全体がロールバックされる</li>
     * </ul>
     * 
     * @param lessonId レッスンID
     * @param requests トレーニングリクエストリスト
     * @throws InvalidRequestException バリデーションエラーが発生した場合
     * @throws ConflictException 既に同じ順序番号のトレーニングが存在する場合
     */
    @Transactional
    public void createTrainings(UUID lessonId, List<TrainingRequest> requests) {
        // lessonIdのnullチェック（Service層は「最後の砦」として機能）
        if (lessonId == null) {
            throw new InvalidRequestException("レッスンIDは必須です");
        }
        
        if (requests == null || requests.isEmpty()) {
            return;
        }
        
        // 同一lessonId内でのorderNo重複チェック（業務ルール）
        Set<Integer> orderNos = new HashSet<>();
        for (TrainingRequest req : requests) {
            Integer orderNo = req.getOrderNo();
            if (orderNo == null) {
                throw new InvalidRequestException("順序番号は必須です");
            }
            if (!orderNos.add(orderNo)) {
                throw new InvalidRequestException("順序番号が重複しています: " + orderNo);
            }
        }
        
        // エンティティ作成（形式的な検証はDTOのBean Validationに委譲）
        List<Training> trainings = new ArrayList<>();
        for (TrainingRequest req : requests) {
            Training.TrainingId trainingId = new Training.TrainingId(lessonId, req.getOrderNo());
            Training training = new Training();
            training.setId(trainingId);
            training.setName(req.getName());
            training.setReps(req.getReps());
            trainings.add(training);
        }
        
        try {
            trainingRepository.saveAll(trainings);
        } catch (DataIntegrityViolationException e) {
            log.warn("Data integrity violation while creating trainings", e);
            // DB制約違反（UNIQUE制約）をキャッチしてConflictExceptionに変換
            // 同時リクエストやリトライ時の二重作成を防ぐ
            throw new ConflictException("このレッスンには既に同じ順序番号のトレーニングが存在します");
        }
    }
    
    /**
     * レッスンIDでトレーニングを取得
     * 
     * @param lessonId レッスンID
     * @return トレーニングレスポンスのリスト
     * @throws InvalidRequestException lessonIdがnullの場合
     */
    public List<TrainingResponse> getTrainingsByLessonId(UUID lessonId) {
        // lessonIdのnullチェック（Service層は「最後の砦」として機能）
        if (lessonId == null) {
            throw new InvalidRequestException("レッスンIDは必須です");
        }
        
        List<Training> trainings = trainingRepository.findByIdLessonIdOrderByIdOrderNoAsc(lessonId);
        return trainings.stream()
            .map(TrainingResponse::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * レッスンIDでトレーニングを削除
     * 
     * <p>パフォーマンス最適化: 全件SELECT → DELETEの代わりに、直接DELETEクエリを実行します。</p>
     * 
     * @param lessonId レッスンID
     * @throws InvalidRequestException lessonIdがnullの場合
     */
    @Transactional
    public void deleteByLessonId(UUID lessonId) {
        // lessonIdのnullチェック（Service層は「最後の砦」として機能）
        if (lessonId == null) {
            throw new InvalidRequestException("レッスンIDは必須です");
        }
        
        trainingRepository.deleteByLessonId(lessonId);
    }
}

