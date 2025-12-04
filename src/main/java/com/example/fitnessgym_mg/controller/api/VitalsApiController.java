package com.example.fitnessgym_mg.controller.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fitnessgym_mg.dto.response.VitalsHistoryResponse;
import com.example.fitnessgym_mg.service.LessonService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 体重/BMI履歴REST APIコントローラー
 * Reactフロントエンドとの連携用
 */
@Slf4j
@RestController
@RequestMapping("/api/customers/{customer_id}/vitals")
@RequiredArgsConstructor
public class VitalsApiController {

    private final LessonService lessonService;

    /**
     * GET /api/customers/{customer_id}/vitals/history
     * 体重・BMIデータ履歴の時系列データ取得
     */
    @GetMapping("/history")
    public ResponseEntity<VitalsHistoryResponse> getVitalsHistory(@PathVariable("customer_id") UUID customerId) {
        try {
            List<VitalsHistoryResponse.VitalsData> data = lessonService.getVitalsHistoryByCustomerId(customerId);
            
            VitalsHistoryResponse response = VitalsHistoryResponse.builder()
                    .data(data)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("体重/BMI履歴取得でエラーが発生しました: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}

