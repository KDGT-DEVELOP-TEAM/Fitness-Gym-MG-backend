package com.example.fitnessgym_mg.controller.api;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fitnessgym_mg.dto.response.HomeResponse;
import com.example.fitnessgym_mg.dto.response.LessonResponse;
import com.example.fitnessgym_mg.service.LessonService;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ホーム/ダッシュボードREST APIコントローラー
 * Reactフロントエンドとの連携用
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HomeApiController {

    private final LessonService lessonService;
    private final SecurityUtil securityUtil;

    /**
     * GET /api/trainers/home
     * 当日・直近(1週間以内)の予約状況/レッスン概要の取得
     */
    @GetMapping("/trainers/home")
    public ResponseEntity<HomeResponse> getTrainerHome() {
        // 現在ログイン中のトレーナーを取得
        UUID trainerId = securityUtil.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("認証されていません"))
                .getId();

        try {
            // 直近1週間のレッスンを取得
            List<LessonResponse> upcomingLessons = lessonService.getUpcomingLessonsByTrainerId(trainerId);

            HomeResponse response = HomeResponse.builder()
                    .upcomingLessons(upcomingLessons)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("トレーナーホームデータ取得でエラーが発生しました: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /api/admin/home
     * 全店舗のレッスン実施回数/レッスン履歴 ==統計情報概要の取得
     */
    @GetMapping("/admin/home")
    public ResponseEntity<HomeResponse> getAdminHome(
            @RequestParam(defaultValue = "month") String chartType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            // レッスン履歴一覧（最新の数件）
            Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
            var lessonPage = lessonService.searchLessons(null, null, pageable);
            List<LessonResponse> recentLessons = lessonPage.getContent();

            // グラフデータ
            var chartData = lessonService.getLessonChartData(null, chartType);

            // 総レッスン数（簡易版：ページネーションの総件数を使用）
            long totalLessonCount = lessonPage.getTotalElements();

            HomeResponse response = HomeResponse.builder()
                    .recentLessons(recentLessons)
                    .totalLessonCount(totalLessonCount)
                    .chartData(chartData)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("管理者ホームデータ取得でエラーが発生しました: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * GET /api/stores/{store_id}/manager/home
     * 所属店舗のレッスン実施回数/レッスン履歴 ==統計情報概要の取得
     */
    @GetMapping("/stores/{store_id}/manager/home")
    public ResponseEntity<HomeResponse> getManagerHome(
            @PathVariable("store_id") UUID storeId,
            @RequestParam(defaultValue = "month") String chartType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            // レッスン履歴一覧（最新の数件）
            Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
            var lessonPage = lessonService.searchLessons(storeId, null, pageable);
            List<LessonResponse> recentLessons = lessonPage.getContent();

            // グラフデータ
            var chartData = lessonService.getLessonChartData(storeId, chartType);

            // 総レッスン数（簡易版：ページネーションの総件数を使用）
            long totalLessonCount = lessonPage.getTotalElements();

            HomeResponse response = HomeResponse.builder()
                    .recentLessons(recentLessons)
                    .totalLessonCount(totalLessonCount)
                    .chartData(chartData)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("店長ホームデータ取得でエラーが発生しました: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}

