package com.example.fitnessgym_mg.exception;

/**
 * 認可エラーが発生した場合にスローされる例外
 * 認証済みユーザーがリソースにアクセスする権限がない場合に使用
 * HTTPステータスコード403 Forbiddenに対応
 */
public class AccessDeniedException extends RuntimeException {
    
    public AccessDeniedException(String message) {
        super(message);
    }
    
    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}

