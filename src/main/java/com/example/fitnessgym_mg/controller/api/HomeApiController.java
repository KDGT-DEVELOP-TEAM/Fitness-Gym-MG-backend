package com.example.fitnessgym_mg.controller.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fitnessgym_mg.config.ApplicationConstants;
import com.example.fitnessgym_mg.dto.response.HomeResponse;
import com.example.fitnessgym_mg.dto.response.LessonResponse;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.ChartPeriod;
import com.example.fitnessgym_mg.exception.InvalidRequestException;
import com.example.fitnessgym_mg.service.AuthorizationFacade;
import com.example.fitnessgym_mg.service.LessonService;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ホーム/ダッシュボードREST APIコントローラー
 * Reactフロントエンドとの連携用
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HomeApiController {

	private final LessonService lessonService;
	private final SecurityUtil securityUtil;
	private final AuthorizationFacade authorizationFacade;

	/**
	 * GET /api/trainers/home
	 * 1週間後～1ヶ月後までのレッスン予定の取得（ページネーション対応）
	 * トレーナーホームページ用
	 */
	@PreAuthorize("hasRole('TRAINER')")
	@GetMapping("/trainers/home")
	public ResponseEntity<HomeResponse> getTrainerHome(
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be 0 or greater") @Max(value = ApplicationConstants.MAX_PAGE_NUMBER, message = "Page is too large") int page,
			@RequestParam(defaultValue = "10") @Min(value = ApplicationConstants.MIN_PAGE_SIZE, message = "Size must be at least 1") @Max(value = ApplicationConstants.MAX_PAGE_SIZE, message = "Size must not exceed 100") int size) {
		// 現在ログイン中のトレーナーを取得
		UUID trainerId = securityUtil.getCurrentUserOrThrow().getId();

		// ページネーション情報を設定
		Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").ascending());

		// 1週間後～1ヶ月後のレッスンを取得（ページネーション対応）
		Page<LessonResponse> lessonPage = lessonService.getUpcomingLessonsByTrainerId(trainerId, pageable);

		HomeResponse response = HomeResponse.builder()
				.upcomingLessons(lessonPage.getContent())
				.totalLessonCount(lessonPage.getTotalElements()) // 総件数を設定
				.build();

		return ResponseEntity.ok(response);
	}

	/**
	 * GET /api/admin/home
	 * 全店舗のレッスン実施回数/レッスン履歴 ==統計情報概要の取得
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/admin/home")
	public ResponseEntity<HomeResponse> getAdminHome(
			@RequestParam(defaultValue = "month") String chartType,
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be 0 or greater") @Max(value = ApplicationConstants.MAX_PAGE_NUMBER, message = "Page is too large") int page,
			@RequestParam(defaultValue = "10") @Min(value = ApplicationConstants.MIN_PAGE_SIZE, message = "Size must be at least 1") @Max(value = ApplicationConstants.MAX_PAGE_SIZE, message = "Size must not exceed 100") int size) {

		// レッスン履歴一覧（最新の数件）
		Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
		var lessonPage = lessonService.searchLessons(null, pageable);
		List<LessonResponse> recentLessons = lessonPage.getContent();

		// グラフデータ（StringからChartPeriodに変換）
		ChartPeriod period = convertToChartPeriod(chartType);
		var chartData = lessonService.getLessonChartData(null, period);

		// 総レッスン数（簡易版：ページネーションの総件数を使用）
		long totalLessonCount = lessonPage.getTotalElements();

		HomeResponse response = HomeResponse.builder()
				.recentLessons(recentLessons)
				.totalLessonCount(totalLessonCount)
				.chartData(chartData)
				.build();

		return ResponseEntity.ok(response);
	}

	/**
	 * GET /api/stores/{store_id}/manager/home
	 * 所属店舗のレッスン実施回数/レッスン履歴 ==統計情報概要の取得
	 */
	@PreAuthorize("hasRole('MANAGER')")
	@GetMapping("/stores/{store_id}/manager/home")
	public ResponseEntity<HomeResponse> getManagerHome(
			@PathVariable("store_id") UUID storeId,
			@RequestParam(defaultValue = "month") String chartType,
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be 0 or greater") @Max(value = ApplicationConstants.MAX_PAGE_NUMBER, message = "Page is too large") int page,
			@RequestParam(defaultValue = "10") @Min(value = ApplicationConstants.MIN_PAGE_SIZE, message = "Size must be at least 1") @Max(value = ApplicationConstants.MAX_PAGE_SIZE, message = "Size must not exceed 100") int size) {

		// 現在のユーザーを取得
		User currentUser = securityUtil.getCurrentUserOrThrow();

		// リソース単位の制御: マネージャーが自分の店舗のみアクセス可能か確認
		authorizationFacade.checkCanAccessStoreOrThrow(currentUser, storeId);

		// レッスン履歴一覧（最新の数件）
		Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
		var lessonPage = lessonService.searchLessons(storeId, pageable);
		List<LessonResponse> recentLessons = lessonPage.getContent();

		// グラフデータ（StringからChartPeriodに変換）
		ChartPeriod period = convertToChartPeriod(chartType);
		var chartData = lessonService.getLessonChartData(storeId, period);

		// 総レッスン数（簡易版：ページネーションの総件数を使用）
		long totalLessonCount = lessonPage.getTotalElements();

		HomeResponse response = HomeResponse.builder()
				.recentLessons(recentLessons)
				.totalLessonCount(totalLessonCount)
				.chartData(chartData)
				.build();

		return ResponseEntity.ok(response);
	}

	/**
	 * String型のchartTypeをChartPeriod enumに変換
	 * 
	 * @param chartType チャートタイプ（"week" or "month"）
	 * @return ChartPeriod enum
	 * @throws InvalidRequestException 不正な値の場合
	 */
	private ChartPeriod convertToChartPeriod(String chartType) {
		if (chartType == null) {
			return ChartPeriod.MONTH; // デフォルト値
		}

		switch (chartType.toLowerCase()) {
		case "week":
			return ChartPeriod.WEEK;
		case "month":
			return ChartPeriod.MONTH;
		default:
			throw new InvalidRequestException("chartType must be 'week' or 'month'");
		}
	}
}
