package com.example.fitnessgym_mg.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 認証関連コントローラー
 * ログイン画面の表示を担当（実際のログイン処理はSpring Securityが自動的に行う）
 */
@Controller
public class AuthController {

    /**
     * ログインフォーム表示
     * 
     * 処理の流れ：
     * 1. リクエストパラメータ（error、logout）を確認
     * 2. errorパラメータがある場合、エラーメッセージをModelに追加
     * 3. logoutパラメータがある場合、ログアウトメッセージをModelに追加
     * 4. ログイン画面のテンプレートを返す
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {
        
        if (error != null) {
            model.addAttribute("errorMessage", "メールアドレスまたはパスワードが正しくありません");
        }
        
        if (logout != null) {
            model.addAttribute("logoutMessage", "ログアウトしました");
        }
        
        return "auth/login";
    }
}

