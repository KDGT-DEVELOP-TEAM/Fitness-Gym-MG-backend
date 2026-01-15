package com.example.fitnessgym_mg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 実装エラーが発生した場合にスローされる例外
 * HTTPステータスコード500 Internal Server Errorに対応
 * 
 * <p>Service層やRepository層で使用することを想定した例外。
 * コードの実装上の問題（インターフェースの実装不足、型の不一致など）を表します。
 * 通常、この例外は開発時に発見され、本番環境では発生しないはずです。</p>
 * 
 * <p>注意: {@code @ResponseStatus}アノテーションは、{@link GlobalExceptionHandler}が
 * 設定されていない場合のフォールバックとして機能します。
 * 通常は{@link GlobalExceptionHandler}がHTTPステータスコードを設定するため、
 * このアノテーションは実質的に使用されませんが、明示的なドキュメントとして残しています。</p>
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ImplementationException extends RuntimeException {
    
    public ImplementationException(String message) {
        super(message);
    }
    
    public ImplementationException(String message, Throwable cause) {
        super(message, cause);
    }
}
