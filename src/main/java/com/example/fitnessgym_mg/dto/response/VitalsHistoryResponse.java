package com.example.fitnessgym_mg.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 体重/BMI履歴レスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalsHistoryResponse {
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VitalsData {
        /**
         * 体重/BMIを記録した日時（レッスン開始日時基準）
         * 
         * <p>レッスン開始日時（{@link com.example.fitnessgym_mg.entity.Lesson#startDate}）を基準として、体重とBMIを記録した日時を表します。</p>
         * <p>タイムゾーン情報を含む実時刻として{@link OffsetDateTime}を使用しています。</p>
         * <p>これにより、異なるタイムゾーンからのアクセスや、時系列表示・比較・集計処理において正確な時刻情報を保持できます。</p>
         */
        private OffsetDateTime date;
        private BigDecimal weight;
        private BigDecimal bmi;
    }
    
    @Builder.Default
    private List<VitalsData> data = List.of();
}

