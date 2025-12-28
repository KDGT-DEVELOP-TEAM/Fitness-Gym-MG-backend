package com.example.fitnessgym_mg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 認証処理中に状態不整合が発生した場合にスローされる例外
 * HTTPステータスコード401 Unauthorizedに対応
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthenticationStateException extends RuntimeException {
    
    public AuthenticationStateException(String message) {
        super(message);
    }
    
    public AuthenticationStateException(String message, Throwable cause) {
        super(message, cause);
    }
}

