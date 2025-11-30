package com.example.fitnessgym_mg.dto.request;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonRequest {
    
    @NotNull(message = "顧客IDは必須です")
    private UUID customerId;
    
    @NotNull(message = "店舗IDは必須です")
    private UUID storeId;
    
    @NotNull(message = "トレーナーIDは必須です")
    private UUID trainerId;
    
    private String condition;
    
    private Double weight;
    
    private String meal;
    
    private String memo;
    
    @NotNull(message = "開始日時は必須です")
    private LocalDateTime startDate;
    
    @NotNull(message = "終了日時は必須です")
    private LocalDateTime endDate;
    
    private LocalDateTime nextDate;
    
    private UUID nextStoreId;
    
    private UUID nextTrainerId;
    
    private List<TrainingRequest> trainings;
}

