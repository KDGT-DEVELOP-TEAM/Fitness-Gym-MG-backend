package com.example.fitnessgym_mg.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonRequest {
    
    @NotNull(message = "店舗IDは必須です")
    private UUID storeId;
    
    @NotNull(message = "トレーナーIDは必須です")
    private UUID trainerId;
    
    @Size(max = 500, message = "コンディションは500文字以内で入力してください")
    private String condition;
    
    @Min(value = 0, message = "体重は0以上である必要があります")
    @Max(value = 500, message = "体重は500以下である必要があります")
    private BigDecimal weight;
    
    @Size(max = 500, message = "食事内容は500文字以内で入力してください")
    private String meal;
    
    @Size(max = 1000, message = "メモは1000文字以内で入力してください")
    private String memo;
    
    @NotNull(message = "開始日時は必須です")
    private LocalDateTime startDate;
    
    @NotNull(message = "終了日時は必須です")
    private LocalDateTime endDate;
    
    private LocalDateTime nextDate;
    
    private UUID nextStoreId;
    
    private UUID nextTrainerId;
    
    private List<TrainingRequest> trainings;
    
    /**
     * 日時範囲の妥当性を検証
     * 終了日時が開始日時より後であることを確認
     */
    @AssertTrue(message = "終了日時は開始日時より後である必要があります")
    private boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true; // @NotNullでチェックされるため
        }
        return endDate.isAfter(startDate);
    }
}

