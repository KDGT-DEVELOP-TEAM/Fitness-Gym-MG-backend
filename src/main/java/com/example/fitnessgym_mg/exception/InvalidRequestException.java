package com.example.fitnessgym_mg.exception;

/**
 * 不正なリクエストを表す例外
 * HTTPステータスコード400 Bad Requestに対応
 * 
 * <p>Controller層で使用することを想定した例外。
 * クライアントの入力ミスや不正なパラメータ指定を表す。</p>
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}

