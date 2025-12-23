package com.example.fitnessgym_mg.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        private LocalDateTime date;
        private BigDecimal weight;
        private BigDecimal bmi;
    }
    
    private List<VitalsData> data;
}

