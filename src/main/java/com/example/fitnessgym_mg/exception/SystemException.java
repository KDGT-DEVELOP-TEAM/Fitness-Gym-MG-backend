package com.example.fitnessgym_mg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * システム内部エラーが発生した場合にスローされる例外
 * HTTPステータスコード500 Internal Server Errorに対応
 * 
 * <p>Service層で使用することを想定した例外。
 * システム内部エラー（API呼び出し失敗、データベースエラー、予期しないエラーなど）を表します。</p>
 * 
 * <p>注意: {@code @ResponseStatus}アノテーションは、{@link GlobalExceptionHandler}が
 * 設定されていない場合のフォールバックとして機能します。
 * 通常は{@link GlobalExceptionHandler}がHTTPステータスコードを設定するため、
 * このアノテーションは実質的に使用されませんが、明示的なドキュメントとして残しています。</p>
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SystemException extends RuntimeException {
    
    public SystemException(String message) {
        super(message);
    }
    
    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
