package com.example.fitnessgym_mg.exception;

/**
 * 認証エラーが発生した場合にスローされる例外
 */
public class AuthenticationException extends RuntimeException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
