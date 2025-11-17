package com.example.fitnessgym_mg.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.fitnessgym_mg.dto.response.LessonResponse;
import com.example.fitnessgym_mg.dto.response.LessonResponse.LessonChartData;
import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.repository.StoreRepository; // 店舗リスト取得用
import com.example.fitnessgym_mg.service.LessonService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/lessons")
public class LessonController {

	private final LessonService lessonService;
	private final StoreRepository storeRepository; // 仮

	// --- レッスン履歴ページ（メインビュー） ---
	@GetMapping
	public String list(
			// 共通絞り込み
			@RequestParam(required = false) String storeId,
			@RequestParam(required = false) String keyword,
			// グラフ切り替え
			@RequestParam(defaultValue = "month") String chartType,
			Model model) {

		// 1. 一覧データ
		List<LessonResponse> lessons = lessonService.searchLessons(storeId, keyword);

		// 2. グラフデータ（初期表示は 'month'）
		LessonResponse.LessonChartData chartData = lessonService.getLessonChartData(storeId, chartType);

		// 3. 店舗リスト（絞り込み用）
		List<Store> stores = (List<Store>) storeRepository.findAll();

		model.addAttribute("lessons", lessons);
		model.addAttribute("count", lessons.size());

		model.addAttribute("stores", stores);
		model.addAttribute("storeId", storeId);
		model.addAttribute("keyword", keyword);

		model.addAttribute("chartData", chartData);
		model.addAttribute("chartType", chartType);

		return "lessons/list"; // Thymeleaf テンプレート名
	}

	// --- グラフデータ取得API (非同期更新用) ---
	@GetMapping("/chart")
	@ResponseBody
	public ResponseEntity<LessonChartData> getChartData(
			@RequestParam(required = false) String storeId,
			@RequestParam(defaultValue = "month") String type) {

		LessonChartData chartData = lessonService.getLessonChartData(storeId, type);
		return ResponseEntity.ok(chartData);
	}
}