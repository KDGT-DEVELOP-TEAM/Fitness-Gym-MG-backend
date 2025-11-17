package com.example.fitnessgym_mg.controller;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.RequiredArgsConstructor;

/**
 * 姿勢画像関連のビューコントローラー
 * 姿勢画像一覧・比較画面の表示を担当
 */
@Controller
@RequiredArgsConstructor
public class PostureViewController {

    /**
     * 姿勢画像一覧ページを表示
     * GET /customers/{customerId}/posture_groups
     * 指定された顧客の姿勢画像グループ一覧を表示
     */
    @GetMapping("/customers/{customerId}/posture_groups")
    public String postureGroupPage(@PathVariable UUID customerId, Model model) {
        model.addAttribute("customerId", customerId);
        return "posture/posture-group";
    }

    /**
     * 姿勢画像比較ページを表示
     * GET /customers/{customerId}/posture/compare
     * 指定された顧客の姿勢画像を左右で比較表示
     */
    @GetMapping("/customers/{customerId}/posture/compare")
    public String postureComparePage(@PathVariable UUID customerId, Model model) {
        model.addAttribute("customerId", customerId);
        return "posture/posture-image";
    }
}


