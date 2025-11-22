package com.example.fitnessgym_mg.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class LessonRequest {

    @NotNull(message = "顧客IDは必須です")
    private UUID customerId;

    @NotNull(message = "店舗IDは必須です")
    private UUID storeId;

    @NotNull(message = "トレーナーIDは必須です")
    private UUID trainerId;

    @Size(max = 50, message = "体調は50文字以内で入力してください")
    private String condition;

    @Positive(message = "体重は正の数である必要があります")
    private BigDecimal weight;

    @Size(max = 150, message = "食事は150文字以内で入力してください")
    private String meal;

    @Size(max = 500, message = "備考は500文字以内で入力してください")
    private String memo;

    @NotNull(message = "レッスン開始日時は必須です")
    private OffsetDateTime startDate;

    @NotNull(message = "レッスン終了日時は必須です")
    private OffsetDateTime endDate;

    private OffsetDateTime nextDate;

    private UUID nextStoreId;

    private UUID nextTrainerId;

    @Valid
    @Size(max = 2, message = "トレーニングは最大2種目まで登録できます")
    private List<TrainingRequest> trainings;
}
