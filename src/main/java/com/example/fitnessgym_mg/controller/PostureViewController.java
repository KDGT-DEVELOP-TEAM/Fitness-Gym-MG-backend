package com.example.fitnessgym_mg.controller;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.RequiredArgsConstructor;

/**
 * 姿勢画像HTMLページコントローラー
 * Thymeleafテンプレートを返し、JavaScriptがAPIからデータ取得
 */
@Controller
@RequiredArgsConstructor
public class PostureViewController {

    /**
     * GET /customers/{customerId}/posture_groups
     * posture/posture-group.htmlを返す（画像一覧ページ）
     * JS内で /api/customers/{customerId}/posture_groups を呼びデータ取得
     */
    @GetMapping("/customers/{customerId}/posture_groups")
    public String postureGroupPage(@PathVariable UUID customerId, Model model) {
        model.addAttribute("customerId", customerId);
        return "posture/posture-group";
    }

    /**
     * GET /customers/{customerId}/posture/compare
     * posture/posture-image.htmlを返す（画像比較ページ）
     * JS内で /api/customers/{customerId}/posture_groups を呼びデータ取得
     */
    @GetMapping("/customers/{customerId}/posture/compare")
    public String postureComparePage(@PathVariable UUID customerId, Model model) {
        model.addAttribute("customerId", customerId);
        return "posture/posture-image";
    }
}


