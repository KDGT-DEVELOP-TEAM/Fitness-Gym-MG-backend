package com.example.fitnessgym_mg.controller.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
	 * 新規レッスン入力フォーム表示
	 * 
	 * 処理の流れ：
	 * 1. 顧客情報を取得
	 * 2. 店舗一覧を取得（管理者は全店舗、店長は所属店舗のみ）
	 * 3. トレーナー一覧を取得
	 * 4. フォーム用の空のリクエストオブジェクトを作成
	 * 5. 画面に表示するデータをModelに設定して返す
	 */
	@GetMapping({ "/admin/lessons/new", "/manager/{storeId}/lessons/new" })
	public String newLessonForm(
			@PathVariable(required = false) UUID storeId,
			@RequestParam UUID customerId,
			Model model) {

		// 顧客情報を取得
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new RuntimeException("顧客が見つかりません"));

		// 店舗一覧を取得（管理者は全店舗、店長は所属店舗のみ）
		List<Store> stores;
		if (storeId != null) {
			stores = List.of(storeRepository.findById(storeId)
					.orElseThrow(() -> new RuntimeException("店舗が見つかりません")));
		} else {
			stores = storeRepository.findAll();
		}

		// トレーナー一覧を取得
		List<User> trainers = userRepository.findAll();

		// 空のリクエストオブジェクトを作成
		LessonRequest lessonRequest = LessonRequest.builder()
				.customerId(customerId)
				.build();

		model.addAttribute("customer", customer);
		model.addAttribute("stores", stores);
		model.addAttribute("trainers", trainers);
		model.addAttribute("lessonRequest", lessonRequest);
		model.addAttribute("storeId", storeId);

		return "lesson/lesson-new";
	}

	/**
	 * POST /admin/lessons
	 * POST /manager/{storeId}/lessons
	 * レッスン保存処理
	 * 
	 * 処理の流れ：
	 * 1. フォームから送信されたデータをバリデーション
	 * 2. バリデーションエラーがある場合、エラーメッセージと共にフォーム画面に戻る
	 * 3. バリデーションが成功した場合、レッスン情報を保存
	 * 4. 保存成功後、成功メッセージを設定してリダイレクト
	 * 5. エラーが発生した場合、エラーメッセージと共にフォーム画面に戻る
	 */
	@PostMapping({ "/admin/lessons", "/manager/{storeId}/lessons" })
	public String createLesson(
			@PathVariable(required = false) UUID storeId,
			@Valid @ModelAttribute LessonRequest request,
			BindingResult result,
			RedirectAttributes redirectAttributes,
			Model model) {

		// バリデーションエラーがある場合はフォームに戻る
		if (result.hasErrors()) {
			// 再表示用のデータを準備
			Customer customer = customerRepository.findById(request.getCustomerId())
					.orElseThrow(() -> new RuntimeException("顧客が見つかりません"));

			List<Store> stores;
			if (storeId != null) {
				stores = List.of(storeRepository.findById(storeId)
						.orElseThrow(() -> new RuntimeException("店舗が見つかりません")));
			} else {
				stores = storeRepository.findAll();
			}

			List<User> trainers = userRepository.findAll();

			model.addAttribute("customer", customer);
			model.addAttribute("stores", stores);
			model.addAttribute("trainers", trainers);
			model.addAttribute("storeId", storeId);

			return "lesson/lesson-new";
		}

		try {
			// レッスン保存
			lessonService.createLesson(request);

			// 成功メッセージ
			redirectAttributes.addFlashAttribute("successMessage", "レッスンを保存しました");

			// レッスン詳細画面へリダイレクト（将来実装）
			// 現在は一覧画面へリダイレクト
			if (storeId != null) {
				return "redirect:/manager/" + storeId + "/lessons";
			} else {
				return "redirect:/admin/lessons";
			}

		} catch (Exception e) {
			// エラーメッセージを設定してフォームに戻る
			model.addAttribute("errorMessage", "レッスンの保存に失敗しました: " + e.getMessage());

			// 再表示用のデータを準備
			Customer customer = customerRepository.findById(request.getCustomerId())
					.orElseThrow(() -> new RuntimeException("顧客が見つかりません"));

			List<Store> stores;
			if (storeId != null) {
				stores = List.of(storeRepository.findById(storeId)
						.orElseThrow(() -> new RuntimeException("店舗が見つかりません")));
			} else {
				stores = storeRepository.findAll();
			}

			List<User> trainers = userRepository.findAll();

			model.addAttribute("customer", customer);
			model.addAttribute("stores", stores);
			model.addAttribute("trainers", trainers);
			model.addAttribute("storeId", storeId);

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
	 * 2. その顧客のレッスン履歴を取得（開始日時の新しい順）
	 * 3. 画面に表示するデータをModelに設定して返す
	 * 4. エラーが発生した場合、エラーメッセージを設定して顧客選択画面にリダイレクト
	 */
	@GetMapping({ "/admin/history/{customerId}", "/manager/{storeId}/history/{customerId}",
			"/trainer/history/{customerId}" })
	public String lessonHistory(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID customerId,
			Model model) {

		try {
			// 顧客情報を取得
			Customer customer = customerRepository.findById(customerId)
					.orElseThrow(() -> new RuntimeException("顧客が見つかりません"));

			// レッスン履歴を取得
			List<LessonResponse> lessons = lessonService.getLessonsByCustomerId(customerId);

			model.addAttribute("customer", customer);
			model.addAttribute("lessons", lessons);
			model.addAttribute("customerId", customerId);
			model.addAttribute("storeId", storeId);

			return "lesson/lesson-list";

		} catch (RuntimeException e) {
			model.addAttribute("errorMessage", "履歴の取得に失敗しました: " + e.getMessage());
			return "redirect:/trainer/customers";
		}
	}

	/**
	 * GET /admin/lessons/{lessonId}
	 * GET /manager/{storeId}/lessons/{lessonId}
	 * レッスン詳細画面表示
	 * 
	 * 処理の流れ：
	 * 1. レッスンIDでレッスン詳細情報を取得（トレーニング種目、姿勢画像を含む）
	 * 2. 画面に表示するデータをModelに設定して返す
	 * 3. エラーが発生した場合、エラーメッセージを設定してリダイレクト
	 */
	@GetMapping({ "/admin/lessons/{lessonId}", "/manager/{storeId}/lessons/{lessonId}" })
	public String lessonDetail(
			@PathVariable(required = false) UUID storeId,
			@PathVariable UUID lessonId,
			Model model) {

		try {
			// レッスン詳細データを取得
			LessonResponse lessonResponse = lessonService.getLessonDetail(lessonId);

			model.addAttribute("lesson", lessonResponse);
			model.addAttribute("storeId", storeId);

			return "lesson/lesson-detail";

		} catch (RuntimeException e) {
			model.addAttribute("errorMessage", "レッスンの取得に失敗しました: " + e.getMessage());

			if (storeId != null) {
				return "redirect:/manager/" + storeId + "/lessons";
			} else {
				return "redirect:/admin/lessons";
			}
		}
	}
}
