package com.example.fitnessgym_mg.controller.util;

import org.springframework.ui.Model;

/**
 * コントローラー層のModel設定ユーティリティ
 * Modelに権限フラグを設定するロジックを共通化
 */
public class ControllerModelUtils {
    
    /**
     * Modelにユーザータイプフラグを設定
     * 
     * @param model Modelオブジェクト
     * @param isAdmin 管理者フラグ
     * @param isManager 店長フラグ
     * @param isTrainer トレーナーフラグ
     */
    public static void setUserTypeFlags(Model model, boolean isAdmin, boolean isManager, boolean isTrainer) {
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isManager", isManager);
        model.addAttribute("isTrainer", isTrainer);
    }
}
