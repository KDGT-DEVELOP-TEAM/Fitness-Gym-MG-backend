package com.example.fitnessgym_mg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * 顧客が論理削除（退会済み）されている場合にスローされる例外
 * HTTPステータスコード403 Forbiddenに対応
 * 
 * <p>注意: {@code @ResponseStatus}アノテーションは、{@link GlobalExceptionHandler}が
 * 設定されていない場合のフォールバックとして機能します。
 * 通常は{@link GlobalExceptionHandler}がHTTPステータスコードを設定するため、
 * このアノテーションは実質的に使用されませんが、明示的なドキュメントとして残しています。</p>
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class CustomerDeletedException extends RuntimeException {
    
    private final UUID customerId;
    
    public CustomerDeletedException(UUID customerId) {
        super("顧客は退会済みです: " + customerId);
        this.customerId = customerId;
    }
    
    public CustomerDeletedException(UUID customerId, String message) {
        super(message);
        this.customerId = customerId;
    }
    
    /**
     * 顧客IDを取得
     * 
     * @return 顧客ID
     */
    public UUID getCustomerId() {
        return customerId;
    }
}
