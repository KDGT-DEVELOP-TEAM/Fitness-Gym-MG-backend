package com.example.fitnessgym_mg.controller.web;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

/**
 * 姿勢画像HTMLページコントローラー
 * Thymeleafテンプレートを返し、JavaScriptがAPIからデータ取得
 * 
 * ルーティング:
 * - Admin: /admin/postures
 * - Manager: /manager/{storeId}/postures
 */
@Controller
@RequiredArgsConstructor
public class PostureWebController {

	private final SecurityUtil securityUtil;

    /**
     * GET /admin/postures?customerId={customerId}
     * posture/posture-group.htmlを返す（画像一覧ページ）
     * JS内で /api/customers/{customerId}/posture_groups を呼びデータ取得
     */
    @GetMapping("/admin/postures")
    public String adminPostureGroupPage(@RequestParam UUID customerId, Model model) {
        model.addAttribute("customerId", customerId);
        model.addAttribute("isAdmin", true);
        model.addAttribute("isManager", false);
        model.addAttribute("isTrainer", false);
        return "posture/posture-group";
    }

    /**
     * GET /manager/{storeId}/postures?customerId={customerId}
     * posture/posture-group.htmlを返す（画像一覧ページ）
     * JS内で /api/customers/{customerId}/posture_groups を呼びデータ取得
     */
    @GetMapping("/manager/{storeId}/postures")
    public String managerPostureGroupPage(
            @PathVariable UUID storeId,
            @RequestParam UUID customerId, 
            Model model) {
        model.addAttribute("storeId", storeId);
        model.addAttribute("customerId", customerId);
        model.addAttribute("isAdmin", false);
        model.addAttribute("isManager", true);
        model.addAttribute("isTrainer", false);
        return "posture/posture-group";
    }

    /**
     * GET /admin/postures/compare?customerId={customerId}
     * posture/posture-image.htmlを返す（画像比較ページ）
     * JS内で /api/customers/{customerId}/posture_groups を呼びデータ取得
     */
    @GetMapping("/admin/postures/compare")
    public String adminPostureComparePage(@RequestParam UUID customerId, Model model) {
        model.addAttribute("customerId", customerId);
        return "posture/posture-image";
    }

    /**
     * GET /manager/{storeId}/postures/compare?customerId={customerId}
     * posture/posture-image.htmlを返す（画像比較ページ）
     * JS内で /api/customers/{customerId}/posture_groups を呼びデータ取得
     */
    @GetMapping("/manager/{storeId}/postures/compare")
    public String managerPostureComparePage(
            @PathVariable UUID storeId,
            @RequestParam UUID customerId,
            Model model) {
        model.addAttribute("storeId", storeId);
        model.addAttribute("customerId", customerId);
        return "posture/posture-image";
    }

    /**
     * GET /customer/{customerId}/posture_groups
     * posture/posture-group.htmlを返す（画像一覧ページ）
     * JS内で /api/customers/{customerId}/posture_groups を呼びデータ取得
     */
    @GetMapping("/customer/{customerId}/posture_groups")
    public String trainerPostureGroupPage(@PathVariable UUID customerId, Model model) {
        model.addAttribute("customerId", customerId);
        model.addAttribute("isAdmin", false);
        model.addAttribute("isManager", false);
        model.addAttribute("isTrainer", true);
        return "posture/posture-group";
    }

    /**
     * GET /customer/{customerId}/posture/compare
     * posture/posture-image.htmlを返す（画像比較ページ）
     * JS内で /api/customers/{customerId}/posture_groups を呼びデータ取得
     */
    @GetMapping("/customer/{customerId}/posture/compare")
    public String trainerPostureComparePage(@PathVariable UUID customerId, Model model) {
        model.addAttribute("customerId", customerId);
        return "posture/posture-image";
    }

    // 後方互換性のため、旧パスも一時的にサポート（将来的に削除予定）
    /**
     * @deprecated 後方互換性のため残されています。/admin/postures または /manager/{storeId}/postures を使用してください。
     */
    @Deprecated
    @GetMapping("/customers/{customerId}/posture_groups")
    public String legacyPostureGroupPage(@PathVariable UUID customerId, Model model) {
        model.addAttribute("customerId", customerId);
        return "posture/posture-group";
    }

    /**
     * @deprecated 後方互換性のため残されています。/admin/postures/compare または /manager/{storeId}/postures/compare を使用してください。
     */
    @Deprecated
    @GetMapping("/customers/{customerId}/posture/compare")
    public String legacyPostureComparePage(@PathVariable UUID customerId, Model model) {
        model.addAttribute("customerId", customerId);
        return "posture/posture-image";
    }
}

