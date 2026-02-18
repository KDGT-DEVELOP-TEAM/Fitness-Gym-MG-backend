package com.example.fitnessgym_mg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ビジネスルール違反を表す例外
 * HTTPステータスコード400 Bad Requestに対応
 * 
 * <p>Service層で使用することを想定した例外。
 * ビジネスロジックの制約違反を表す（例: 店長は店舗を1つだけ選択する必要がある）。</p>
 * 
 * <p>注意: {@code @ResponseStatus}アノテーションは、{@link GlobalExceptionHandler}が
 * 設定されていない場合のフォールバックとして機能します。
 * 通常は{@link GlobalExceptionHandler}がHTTPステータスコードを設定するため、
 * このアノテーションは実質的に使用されませんが、明示的なドキュメントとして残しています。</p>
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}

