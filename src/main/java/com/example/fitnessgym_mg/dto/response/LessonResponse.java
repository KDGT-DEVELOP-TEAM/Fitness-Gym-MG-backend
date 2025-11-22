package com.example.fitnessgym_mg.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {
    
    private UUID id;
    
    // 関連エンティティ情報
    private UUID customerId;
    private String customerName;
    private BigDecimal customerHeight;  // BMI計算用
    
    private UUID storeId;
    private String storeName;
    
    private UUID trainerId;
    private String trainerName;
    
    // レッスン情報
    private String condition;
    private BigDecimal weight;
    private String meal;
    private String memo;
    
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    
    // 次回予約情報
    private OffsetDateTime nextDate;
    private UUID nextStoreId;
    private String nextStoreName;
    private UUID nextTrainerId;
    private String nextTrainerName;
    
    private OffsetDateTime createdAt;
    
    // 関連データ
    private List<TrainingResponse> trainings;
    private List<PostureImageResponse> postureImages;
    
    /**
     * BMI計算（体重 ÷ 身長² ）
     * 体重または身長がnullの場合はnullを返す
     */
    public BigDecimal getBmi() {
        if (weight == null || customerHeight == null || customerHeight.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        // BMI = 体重(kg) ÷ (身長(m) × 身長(m))
        // 身長はcmで保存されているため100で割る
        BigDecimal heightInMeters = customerHeight.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weight.divide(heightInMeters.multiply(heightInMeters), 2, RoundingMode.HALF_UP);
    }
}
