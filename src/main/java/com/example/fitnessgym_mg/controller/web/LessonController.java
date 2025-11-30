package com.example.fitnessgym_mg.controller.web;

import java.util.List;
import java.util.UUID;

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
import com.example.fitnessgym_mg.entity.Lesson;
import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.repository.StoreRepository;
import com.example.fitnessgym_mg.repository.UserRepository;
import com.example.fitnessgym_mg.service.LessonService;

import jakarta.validation.Valid;
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
     */
    @GetMapping({"/admin/lessons/new", "/manager/{storeId}/lessons/new"})
    public String newLessonForm(
            @PathVariable(required = false) UUID storeId,
            @RequestParam UUID customerId,
            Model model) {
        
        // 顧客情報を取得
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("顧客が見つかりません"));
        
        // 店舗一覧を取得
        List<Store> stores;
        if (storeId != null) {
            // Manager: 所属店舗のみ
            stores = List.of(storeRepository.findById(storeId)
                    .orElseThrow(() -> new RuntimeException("店舗が見つかりません")));
        } else {
            // Admin: 全店舗
            stores = storeRepository.findAll();
        }
        
        // トレーナー一覧を取得（全ユーザー、実際は権限でフィルタすべき）
        List<User> trainers = userRepository.findAll();
        
        // 空のリクエストオブジェクトを作成
        LessonRequest lessonRequest = LessonRequest.builder()
                .customerId(customerId)
                .build();
        
        // データをモデルに設定
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
     */
    @PostMapping({"/admin/lessons", "/manager/{storeId}/lessons"})
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
            Lesson savedLesson = lessonService.createLesson(request);
            
            // 成功メッセージ
            redirectAttributes.addFlashAttribute("successMessage", "レッスンを保存しました");
            
            // レッスン詳細画面へリダイレクト（将来実装）
            // 現在は一覧画面へリダイレクト（一覧画面がない場合は顧客詳細へ）
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
     * GET /admin/lessons/{lessonId}
     * GET /manager/{storeId}/lessons/{lessonId}
     * レッスン詳細画面表示
     */
    @GetMapping({"/admin/lessons/{lessonId}", "/manager/{storeId}/lessons/{lessonId}"})
    public String lessonDetail(
            @PathVariable(required = false) UUID storeId,
            @PathVariable UUID lessonId,
            Model model) {
        
        try {
            // レッスン詳細データを取得
            LessonResponse lessonResponse = lessonService.getLessonDetail(lessonId);
            
            // モデルに設定
            model.addAttribute("lesson", lessonResponse);
            model.addAttribute("storeId", storeId);
            
            return "lesson/lesson-detail";
            
        } catch (RuntimeException e) {
            // エラーメッセージを設定
            model.addAttribute("errorMessage", "レッスンの取得に失敗しました: " + e.getMessage());
            
            // エラー画面またはリダイレクト
            if (storeId != null) {
                return "redirect:/manager/" + storeId + "/lessons";
            } else {
                return "redirect:/admin/lessons";
            }
        }
    }
}

