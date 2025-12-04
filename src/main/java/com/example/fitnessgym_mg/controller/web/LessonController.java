package com.example.fitnessgym_mg.controller.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;

import com.example.fitnessgym_mg.dto.request.LessonRequest;
import com.example.fitnessgym_mg.dto.response.LessonResponse;
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.repository.StoreRepository;
import com.example.fitnessgym_mg.repository.UserRepository;
import com.example.fitnessgym_mg.service.LessonService;

import lombok.RequiredArgsConstructor;

/**
 * レッスン管理画面コントローラー
 * 新規レッスン入力機能を提供
 */
@Controller
@RequiredArgsConstructor
public class LessonController {

	private final LessonService lessonService;
	private final CustomerRepository customerRepository;
	private final StoreRepository storeRepository;
	private final UserRepository userRepository;

	/**
	 * GET /admin/lessons/new
	 * GET /manager/{storeId}/lessons/new
	 * GET /customer/{customerId}/lesson/new
	 * 新規レッスン入力フォーム表示
	 * 
	 * 処理の流れ：
	 * 1. 顧客情報を取得
	 * 2. 店舗一覧を取得（管理者は全店舗、店長は所属店舗のみ、トレーナーは所属店舗のみ）
	 * 3. トレーナー一覧を取得（トレーナーの場合はログインユーザー自身のみ）
	 * 4. フォーム用の空のリクエストオブジェクトを作成
	 * 5. 画面に表示するデータをModelに設定して返す
	 */
	@GetMapping({ "/admin/lessons/new", "/manager/{storeId}/lessons/new", "/customer/{customerId}/lesson/new" })
	public String newLessonForm(
			@PathVariable(required = false) UUID storeId,
			@PathVariable(required = false) UUID customerId,
			@RequestParam(required = false) UUID customerIdParam,
			HttpServletRequest request,
			Model model) {

		// customerIdを取得（パスパラメータまたはクエリパラメータから）
		UUID actualCustomerId = customerId != null ? customerId : customerIdParam;
		if (actualCustomerId == null) {
			throw new RuntimeException("顧客IDが指定されていません");
		}

		// 顧客情報を取得
		Customer customer = customerRepository.findById(actualCustomerId)
				.orElseThrow(() -> new RuntimeException("顧客が見つかりません"));

		// リクエストパスから判定
		String requestPath = request.getRequestURI();
		boolean isTrainer = requestPath.startsWith("/customer/");

		// 店舗一覧を取得
		List<Store> stores;
		if (isTrainer) {
			// トレーナーの場合：ログインユーザーの所属店舗のみ
			User currentUser = getCurrentUser();
			if (currentUser.getStores() != null && !currentUser.getStores().isEmpty()) {
				stores = new java.util.ArrayList<>(currentUser.getStores());
			} else {
				stores = List.of();
			}
		} else if (storeId != null) {
			// 店長の場合：所属店舗のみ
			stores = List.of(storeRepository.findById(storeId)
					.orElseThrow(() -> new RuntimeException("店舗が見つかりません")));
		} else {
			// 管理者の場合：全店舗
			stores = storeRepository.findAll();
		}

		// トレーナー一覧を取得
		List<User> trainers;
		if (isTrainer) {
			// トレーナーの場合：ログインユーザー自身のみ
			User currentUser = getCurrentUser();
			trainers = List.of(currentUser);
		} else {
			// 管理者・店長の場合：全トレーナー
			trainers = userRepository.findAll();
		}

		// 空のリクエストオブジェクトを作成
		LessonRequest lessonRequest = LessonRequest.builder()
				.customerId(actualCustomerId)
				.build();

		// トレーナーの場合、デフォルト値を設定
		if (isTrainer && !trainers.isEmpty()) {
			lessonRequest.setTrainerId(trainers.get(0).getId());
		}

		model.addAttribute("customer", customer);
		model.addAttribute("stores", stores);
		model.addAttribute("trainers", trainers);
		model.addAttribute("lessonRequest", lessonRequest);
		model.addAttribute("storeId", storeId);
		model.addAttribute("customerId", actualCustomerId);
		model.addAttribute("isTrainer", isTrainer);

		return "lesson/lesson-new";
	}

	/**
	 * POST /admin/lessons
	 * POST /manager/{storeId}/lessons
	 * POST /customer/{customerId}/lessons
	 * レッスン保存処理
	 * 
	 * 処理の流れ：
	 * 1. フォームから送信されたデータをバリデーション
	 * 2. バリデーションエラーがある場合、エラーメッセージと共にフォーム画面に戻る
	 * 3. バリデーションが成功した場合、レッスン情報を保存
	 * 4. 保存成功後、成功メッセージを設定してリダイレクト
	 * 5. エラーが発生した場合、エラーメッセージと共にフォーム画面に戻る
	 */
	@PostMapping({ "/admin/lessons", "/manager/{storeId}/lessons", "/customer/{customerId}/lessons" })
	public String createLesson(
			@PathVariable(required = false) UUID storeId,
			@PathVariable(required = false) UUID customerId,
			@Valid @ModelAttribute LessonRequest request,
			BindingResult result,
			RedirectAttributes redirectAttributes,
			HttpServletRequest httpRequest,
			Model model) {

		// customerIdをパスパラメータから取得（トレーナーの場合）
		if (customerId != null) {
			request.setCustomerId(customerId);
		}

		// リクエストパスから判定
		String requestPath = httpRequest.getRequestURI();
		boolean isTrainer = requestPath.startsWith("/customer/");

		// バリデーションエラーがある場合はフォームに戻る
		if (result.hasErrors()) {
			// 再表示用のデータを準備
			Customer customer = customerRepository.findById(request.getCustomerId())
					.orElseThrow(() -> new RuntimeException("顧客が見つかりません"));

			List<Store> stores;
			List<User> trainers;
			if (isTrainer) {
				// トレーナーの場合
				User currentUser = getCurrentUser();
				if (currentUser.getStores() != null && !currentUser.getStores().isEmpty()) {
					stores = new java.util.ArrayList<>(currentUser.getStores());
				} else {
					stores = List.of();
				}
				trainers = List.of(currentUser);
			} else if (storeId != null) {
				// 店長の場合
				stores = List.of(storeRepository.findById(storeId)
						.orElseThrow(() -> new RuntimeException("店舗が見つかりません")));
				trainers = userRepository.findAll();
			} else {
				// 管理者の場合
				stores = storeRepository.findAll();
				trainers = userRepository.findAll();
			}

			model.addAttribute("customer", customer);
			model.addAttribute("stores", stores);
			model.addAttribute("trainers", trainers);
			model.addAttribute("storeId", storeId);
			model.addAttribute("customerId", request.getCustomerId());
			model.addAttribute("isTrainer", isTrainer);

			return "lesson/lesson-new";
		}

		try {
			// レッスン保存
			lessonService.createLesson(request);

			// 成功メッセージ
			redirectAttributes.addFlashAttribute("successMessage", "レッスンを保存しました");

			// リダイレクト先を決定
			if (isTrainer) {
				// トレーナーの場合：新規レッスン入力画面に戻る
				return "redirect:/customer/" + request.getCustomerId() + "/lesson/new";
			} else if (storeId != null) {
				// 店長の場合
				return "redirect:/manager/" + storeId + "/lessons";
			} else {
				// 管理者の場合
				return "redirect:/admin/lessons";
			}

		} catch (Exception e) {
			// エラーメッセージを設定してフォームに戻る
			model.addAttribute("errorMessage", "レッスンの保存に失敗しました: " + e.getMessage());

			// 再表示用のデータを準備
			Customer customer = customerRepository.findById(request.getCustomerId())
					.orElseThrow(() -> new RuntimeException("顧客が見つかりません"));

			List<Store> stores;
			List<User> trainers;
			if (isTrainer) {
				// トレーナーの場合
				User currentUser = getCurrentUser();
				if (currentUser.getStores() != null && !currentUser.getStores().isEmpty()) {
					stores = new java.util.ArrayList<>(currentUser.getStores());
				} else {
					stores = List.of();
				}
				trainers = List.of(currentUser);
			} else if (storeId != null) {
				// 店長の場合
				stores = List.of(storeRepository.findById(storeId)
						.orElseThrow(() -> new RuntimeException("店舗が見つかりません")));
				trainers = userRepository.findAll();
			} else {
				// 管理者の場合
				stores = storeRepository.findAll();
				trainers = userRepository.findAll();
			}

			model.addAttribute("customer", customer);
			model.addAttribute("stores", stores);
			model.addAttribute("trainers", trainers);
			model.addAttribute("storeId", storeId);
			model.addAttribute("customerId", request.getCustomerId());
			model.addAttribute("isTrainer", isTrainer);

			return "lesson/lesson-new";
		}
	}

	/**
	 * GET /admin/history/{customerId}
	 * GET /manager/{storeId}/history/{customerId}
	 * GET /trainer/history/{customerId}
	 * レッスン履歴一覧画面表示
	 * 
	 * 処理の流れ：
	 * 1. 顧客情報を取得
	 * 2. その顧客のレッスン履歴を取得（ページネーション対応、開始日時の新しい順）
	 * 3. グラフデータを取得
	 * 4. 画面に表示するデータをModelに設定して返す
	 * 5. エラーが発生した場合、エラーメッセージを設定して顧客選択画面にリダイレクト
	 */
	@GetMapping({ "/admin/history/{customerId}", "/manager/{storeId}/history/{customerId}",
			"/customer/{customerId}/lessons" })
	public String lessonHistory(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID customerId,
			@RequestParam(required = false) String from,
			@RequestParam(defaultValue = "month") String chartType,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			HttpServletRequest request,
			Model model) {

		try {
			// 顧客情報を取得
			Customer customer = customerRepository.findById(customerId)
					.orElseThrow(() -> new RuntimeException("顧客が見つかりません"));

			// ページネーション対応のレッスン履歴を取得
			Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
			var lessonPage = lessonService.getLessonsByCustomerId(customerId, pageable);

			// グラフデータを取得
			var chartData = lessonService.getLessonChartDataByCustomerId(customerId, chartType);

			// BASE_PATHを設定（リクエストパスから判定）
			String requestPath = request.getRequestURI();
			String basePath;
			String detailBasePath;
			if (requestPath.startsWith("/manager/")) {
				basePath = "/manager/" + storeId + "/history/" + customerId;
				detailBasePath = "/manager/" + storeId + "/lessons";
			} else if (requestPath.startsWith("/customer/")) {
				basePath = "/customer/" + customerId + "/lessons";
				detailBasePath = "/lesson"; // pas.mdの定義に従い /lesson/{lessonId} を使用
			} else {
				basePath = "/admin/history/" + customerId;
				detailBasePath = "/admin/lessons";
			}

			model.addAttribute("customer", customer);
			model.addAttribute("lessonPage", lessonPage);
			model.addAttribute("lessons", lessonPage.getContent()); // 互換性のため
			model.addAttribute("count", lessonPage.getTotalElements());
			model.addAttribute("customerId", customerId);
			model.addAttribute("storeId", storeId);
			model.addAttribute("chartData", chartData);
			model.addAttribute("chartType", chartType);
			model.addAttribute("BASE_PATH", basePath);
			model.addAttribute("DETAIL_BASE_PATH", detailBasePath);
			model.addAttribute("stores", List.of()); // トレーナーは店舗選択不要

			// ユーザータイプを判定してフラグを設定
			boolean isTrainer = requestPath.startsWith("/customer/");
			boolean isManager = requestPath.startsWith("/manager/");
			boolean isAdmin = !isTrainer && !isManager;
			
			model.addAttribute("isTrainer", isTrainer);
			model.addAttribute("isManager", isManager);
			model.addAttribute("isAdmin", isAdmin);
			
			// 顧客選択後の画面であることを示すフラグ
			model.addAttribute("isCustomerHistoryPage", true);
			
			// 遷移元をModelに追加
			model.addAttribute("from", from);
			
			// 戻る先のURLを設定
			String backUrl = null;
			if (from != null) {
				if (requestPath.startsWith("/manager/")) {
					if ("lessons".equals(from)) {
						backUrl = "/manager/" + storeId + "/lessons";
					} else if ("customers".equals(from)) {
						backUrl = "/manager/" + storeId + "/customers";
					}
				} else if (requestPath.startsWith("/trainer/")) {
					// トレーナーの場合は顧客選択画面に戻る
					backUrl = "/trainer/customers";
				} else {
					// adminの場合
					if ("lessons".equals(from)) {
						backUrl = "/admin/lessons";
					} else if ("customers".equals(from)) {
						backUrl = "/admin/customers";
					}
				}
			}
			model.addAttribute("backUrl", backUrl);
			
			// 顧客選択後の画面では統計情報（グラフ）を非表示にするため、chartDataをnullに設定
			model.addAttribute("chartData", null);

			return "lesson/lesson-list";

		} catch (Exception e) {
			// すべての例外をキャッチしてログ出力
			e.printStackTrace();
			model.addAttribute("errorMessage", "履歴の取得に失敗しました: " + e.getMessage());
			return "redirect:/trainer/customers";
		}
	}

	/**
	 * GET /admin/lessons/{lessonId}
	 * GET /manager/{storeId}/lessons/{lessonId}
	 * GET /trainer/lessons/{lessonId}
	 * GET /lesson/{lessonId}
	 * レッスン詳細画面表示
	 * 
	 * 処理の流れ：
	 * 1. レッスンIDでレッスン詳細情報を取得（トレーニング種目、姿勢画像を含む）
	 * 2. 画面に表示するデータをModelに設定して返す
	 * 3. エラーが発生した場合、エラーメッセージを設定してリダイレクト
	 */
	@GetMapping({ "/admin/lessons/{lessonId}", "/manager/{storeId}/lessons/{lessonId}",
			"/trainer/lessons/{lessonId}", "/lesson/{lessonId}" })
	public String lessonDetail(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID lessonId,
			HttpServletRequest request,
			Model model) {

		try {
			// レッスン詳細データを取得
			LessonResponse lessonResponse = lessonService.getLessonDetail(lessonId);

			// リクエストパスから判定してユーザータイプを設定
			String requestPath = request.getRequestURI();
			boolean isTrainer = requestPath.startsWith("/customer/") || requestPath.startsWith("/trainer/") || requestPath.startsWith("/lesson/");
			boolean isManager = requestPath.startsWith("/manager/");
			boolean isAdmin = !isTrainer && !isManager;

			// サイドバー表示に必要なフラグを設定
			model.addAttribute("isTrainer", isTrainer);
			model.addAttribute("isManager", isManager);
			model.addAttribute("isAdmin", isAdmin);
			model.addAttribute("isCustomerHistoryPage", true); // 履歴一覧の括りとして表示
			
			// 顧客IDをレッスン情報から取得して設定（サイドバーのリンクで使用）
			UUID customerId = lessonResponse.getCustomerId();
			model.addAttribute("customerId", customerId);
			model.addAttribute("storeId", storeId);
			model.addAttribute("lesson", lessonResponse);

			return "lesson/lesson-detail";

		} catch (RuntimeException e) {
			model.addAttribute("errorMessage", "レッスンの取得に失敗しました: " + e.getMessage());

			// リクエストパスから判定してリダイレクト先を決定
			String requestPath = request.getRequestURI();
			if (requestPath.startsWith("/manager/")) {
				return "redirect:/manager/" + storeId + "/lessons";
			} else if (requestPath.startsWith("/trainer/") || requestPath.startsWith("/lesson/")) {
				// トレーナーの場合は、レッスンから顧客IDを取得して履歴ページにリダイレクト
				// ただし、レッスンが見つからない場合は顧客選択画面にリダイレクト
				return "redirect:/trainer/customers";
			} else {
				return "redirect:/admin/lessons";
			}
		}
	}

	/**
	 * 現在ログイン中のユーザーを取得
	 */
	private User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String email = authentication.getName();
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("ログインユーザーが見つかりません"));
	}
}
