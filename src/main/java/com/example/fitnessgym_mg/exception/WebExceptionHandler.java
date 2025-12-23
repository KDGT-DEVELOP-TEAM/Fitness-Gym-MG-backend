package com.example.fitnessgym_mg.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Webコントローラー用のグローバル例外ハンドラー
 * Thymeleafテンプレートを返すWebコントローラーで発生した例外を処理
 */
@Slf4j
@ControllerAdvice
public class WebExceptionHandler {

    /**
     * RuntimeExceptionのハンドリング
     * Webコントローラーで発生した例外をエラー画面にリダイレクト
     * 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
     */
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException e, Model model) {
        log.error("Runtime error in web controller: {}", e.getMessage(), e);
        // 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
        model.addAttribute("errorMessage", "エラーが発生しました");
        return "error/error";
    }

    /**
     * IllegalArgumentExceptionのハンドリング
     * バリデーションエラーなど
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e, Model model) {
        log.warn("Validation error in web controller: {}", e.getMessage());
        model.addAttribute("errorMessage", "入力値が不正です: " + e.getMessage());
        return "error/error";
    }

    /**
     * EntityNotFoundExceptionのハンドリング
     * エンティティが見つからない場合
     * 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public String handleEntityNotFound(EntityNotFoundException e, Model model) {
        log.warn("Entity not found in web controller: {}", e.getMessage());
        // 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
        model.addAttribute("errorMessage", "リソースが見つかりません");
        return "error/404";
    }

    /**
     * AuthenticationExceptionのハンドリング
     * 認証エラーが発生した場合
     * 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
     */
    @ExceptionHandler(AuthenticationException.class)
    public String handleAuthenticationException(AuthenticationException e, Model model) {
        log.warn("Authentication error in web controller: {}", e.getMessage());
        // 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
        model.addAttribute("errorMessage", "認証エラーが発生しました");
        return "error/error";
    }
}
