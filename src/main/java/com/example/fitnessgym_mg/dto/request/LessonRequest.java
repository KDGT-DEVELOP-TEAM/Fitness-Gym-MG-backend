package com.example.fitnessgym_mg.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.example.fitnessgym_mg.config.ApplicationConstants;
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
    
    /**
     * 体重（kg）
     * 
     * <p>有効範囲: 30.0kg以上、300.0kg以下</p>
     */
    @DecimalMin(value = "30.0", inclusive = true, message = "体重は30kg以上である必要があります")
    @DecimalMax(value = "300.0", inclusive = true, message = "体重は300kg以下である必要があります")
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
    
    /**
     * トレーニングリスト
     * 
     * <p>1レッスンあたり最大{@link ApplicationConstants#MAX_LESSON_TRAININGS_COUNT}件まで指定できます。</p>
     * <p>パフォーマンス・DoS対策のため、件数制限を設けています。</p>
     */
    @Size(max = ApplicationConstants.MAX_LESSON_TRAININGS_COUNT, message = "トレーニングは最大" + ApplicationConstants.MAX_LESSON_TRAININGS_COUNT + "件まで指定できます")
    private List<TrainingRequest> trainings;
    
    /**
     * 日時範囲の妥当性を検証
     * 終了日時が開始日時より後であることを確認
     * 
     * <p>Bean Validation仕様に準拠するため、このメソッドはpublicである必要があります。</p>
     */
    @AssertTrue(message = "終了日時は開始日時より後である必要があります")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true; // @NotNullでチェックされるため
        }
        return endDate.isAfter(startDate);
    }
    
    /**
     * 開始日時が未来でないことを検証
     * 
     * <p>レッスンの開始日時は現在の日時より未来に設定できません。
     * これは業務ルールとして、レッスンは実施済み（過去または現在）のものを記録することを前提としています。</p>
     * 
     * <p>タイムゾーン: UTC基準で検証します（DBに保存されているLocalDateTimeがUTCとして扱われるため）。</p>
     * 
     * <p>セキュリティ: Bean Validationによる第一層の防御として機能します。
     * サービス層でも再チェックを行うことで、二重の防御を実現します。</p>
     * 
     * <p>Bean Validation仕様に準拠するため、このメソッドはpublicである必要があります。</p>
     * 
     * @return True if startDate is null or not in the future
     */
    @AssertTrue(message = "開始日時は現在の日時より未来に設定できません")
    public boolean isValidStartDateNotFuture() {
        if (startDate == null) {
            return true; // @NotNullでチェックされるため
        }
        // UTC基準で現在時刻を取得して比較
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return !startDate.isAfter(now);
    }

    /**
     * 終了日時が未来でないことを検証
     * 
     * <p>レッスンの終了日時は現在の日時より未来に設定できません。
     * これは業務ルールとして、レッスンは実施済み（過去または現在）のものを記録することを前提としています。</p>
     * 
     * <p>タイムゾーン: UTC基準で検証します（DBに保存されているLocalDateTimeがUTCとして扱われるため）。</p>
     * 
     * <p>セキュリティ: Bean Validationによる第一層の防御として機能します。
     * サービス層でも再チェックを行うことで、二重の防御を実現します。</p>
     * 
     * <p>Bean Validation仕様に準拠するため、このメソッドはpublicである必要があります。</p>
     * 
     * @return True if endDate is null or not in the future
     */
    @AssertTrue(message = "終了日時は現在の日時より未来に設定できません")
    public boolean isValidEndDateNotFuture() {
        if (endDate == null) {
            return true; // @NotNullでチェックされるため
        }
        // UTC基準で現在時刻を取得して比較
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return !endDate.isAfter(now);
    }

    /**
     * 次回予約フィールドの相関制約を検証
     * 
     * <p>次回予約を設定する場合、日時・店舗・トレーナーはすべて必須です。</p>
     * <p>次回予約を設定しない場合、すべてのフィールドがnullである必要があります。</p>
     * 
     * <p>Bean Validation仕様に準拠するため、このメソッドはpublicである必要があります。</p>
     */
    @AssertTrue(message = "次回予約を設定する場合、日時・店舗・トレーナーはすべて必須です")
    public boolean isValidNextLesson() {
        boolean anySet = nextDate != null || nextStoreId != null || nextTrainerId != null;
        boolean allSet = nextDate != null && nextStoreId != null && nextTrainerId != null;
        return !anySet || allSet;
    }
}

