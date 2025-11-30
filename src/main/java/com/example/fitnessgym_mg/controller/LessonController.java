package com.example.fitnessgym_mg.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.fitnessgym_mg.dto.response.LessonResponse.LessonChartData;
import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.repository.StoreRepository;
import com.example.fitnessgym_mg.service.LessonService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class LessonController {

	private final LessonService lessonService;
	private final StoreRepository storeRepository;

	// --- 1. 本部管理者 (Admin) 用：全店舗のレッスン履歴ページ ---
	@GetMapping("/admin/lessons")
	public String listAdmin(
			@RequestParam(required = false) UUID storeId, // 絞り込み対象の店舗ID (null可)
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "month") String chartType,
			@RequestParam(defaultValue = "0") int page, // ページ番号
			@RequestParam(defaultValue = "10") int size, // ページサイズ
			Model model) {

		// Pageableの生成: レッスン実施日時(startDate)で降順ソートを強制
		PageRequest pageable = PageRequest.of(page, size, Sort.by("startDate").descending());

		// 全店舗リストを取得 (絞り込みUI用)
		List<Store> allStores = storeRepository.findAll();
		model.addAttribute("stores", allStores);

		// サービス層の検索とグラフデータ取得を呼び出し (storeIdがnullなら全店舗が対象)
		return loadLessonData(storeId, keyword, chartType, pageable, model, "/admin/lessons");
	}

	// --- 2. 店長 (Manager) 用：所属店舗のレッスン履歴ページ ---
	@GetMapping("/manager/{storeId}/lessons")
	public String listManager(
			@PathVariable UUID storeId, // URLから所属店舗IDを必須取得
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "month") String chartType,
			@RequestParam(defaultValue = "0") int page, // ページ番号
			@RequestParam(defaultValue = "10") int size, // ページサイズ
			Model model) {

		// Pageableの生成: レッスン実施日時(startDate)で降順ソートを強制
		PageRequest pageable = PageRequest.of(page, size, Sort.by("startDate").descending());

		// ★ 権限チェック: URLのstoreIdがデータベースに存在するか確認 ★
		Store targetStore = storeRepository.findById(storeId)
				.orElseThrow(() -> new RuntimeException("アクセス権限のない店舗ID、または店舗が見つかりません: " + storeId));

		// 店長は自分の店舗データのみを閲覧できるため、絞り込みリストは自分の店舗のみとする
		model.addAttribute("stores", List.of(targetStore));

		// サービス層の検索とグラフデータ取得を呼び出し (storeIdは必須)
		return loadLessonData(storeId, keyword, chartType, pageable, model, "/manager/" + storeId + "/lessons");
	}

	// --- 共通データロードヘルパーメソッド ---
	private String loadLessonData(
			UUID storeId,
			String keyword,
			String chartType,
			Pageable pageable,
			Model model,
			String basePath) {

		// 1. 一覧データ (Pageオブジェクトを取得)
		// LessonServiceはUUID storeIdを受け取る
		var lessonPage = lessonService.searchLessons(storeId, keyword, pageable);

		// 2. グラフデータ
		// LessonServiceはUUID storeIdを受け取る
		LessonChartData chartData = lessonService.getLessonChartData(storeId, chartType);

		// Modelにページネーション情報を渡す
		model.addAttribute("lessonPage", lessonPage);
		// テンプレートで既存の ${lessons} や ${count} を使用している可能性があるため、互換性のために残す
		model.addAttribute("lessons", lessonPage.getContent());
		model.addAttribute("count", lessonPage.getTotalElements());

		// 絞り込みパラメータをViewに渡す
		model.addAttribute("storeId", storeId);
		model.addAttribute("keyword", keyword);

		model.addAttribute("chartData", chartData);
		model.addAttribute("chartType", chartType);

		// View側でのリンク構築に利用 (Admin/Managerパスの切り替え)
		model.addAttribute("BASE_PATH", basePath);

		return "lessons/list"; // 共通のThymeleafテンプレート
	}

	// --- グラフデータ取得API (非同期更新用) ---
	// AdminとManagerの両方のパスを受け付ける
	@GetMapping({ "/admin/lessons/chart", "/manager/{storeId}/lessons/chart" })
	@ResponseBody
	public ResponseEntity<LessonChartData> getChartData(
			@PathVariable(required = false) UUID storeId, // Managerアクセス時のみURLパスから取得
			@RequestParam(required = false) UUID filterStoreId, // Adminが店舗で絞り込む場合のID
			@RequestParam(defaultValue = "month") String type) {

		// Managerアクセス時はURLパスのstoreIdが最優先の絞り込み条件になる
		// Adminアクセス時はstoreIdはnullなので、filterStoreId (クエリパラメータ) を使用
		UUID effectiveStoreId = (storeId != null) ? storeId : filterStoreId;

		LessonChartData chartData = lessonService.getLessonChartData(effectiveStoreId, type);
		return ResponseEntity.ok(chartData);
	}
}