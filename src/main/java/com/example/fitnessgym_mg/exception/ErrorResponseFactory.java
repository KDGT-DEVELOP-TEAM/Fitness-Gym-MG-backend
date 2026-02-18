package com.example.fitnessgym_mg.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * エラーレスポンス生成ファクトリ
 * 
 * <p>エラーレスポンス生成の責務を分離し、一貫性を保つためのファクトリクラスです。</p>
 * <p>GlobalExceptionHandlerからエラーレスポンス生成ロジックを分離することで、
 * テストの容易性と保守性を向上させます。</p>
 */
public class ErrorResponseFactory {
    
    /**
     * エラーレスポンスを生成（単一メッセージ）
     * 
     * @param status HTTPステータスコード
     * @param errorCode エラーコード
     * @param message エラーメッセージ
     * @return ResponseEntity<ErrorResponse>
     */
    public static ResponseEntity<ErrorResponse> create(HttpStatus status, String errorCode, String message) {
        return ResponseEntity
            .status(status)
            .body(new ErrorResponse(errorCode, message));
    }
    
    /**
     * エラーレスポンスを生成（複数メッセージ）
     * 
     * @param status HTTPステータスコード
     * @param errorCode エラーコード
     * @param message エラーメッセージ（最初のメッセージ）
     * @param errors エラーメッセージのリスト
     * @return ResponseEntity<ErrorResponse>
     */
    public static ResponseEntity<ErrorResponse> create(
            HttpStatus status, 
            String errorCode, 
            String message, 
            List<String> errors) {
        return ResponseEntity
            .status(status)
            .body(new ErrorResponse(errorCode, message, errors));
    }
    
    /**
     * バリデーションエラーレスポンスを生成
     * 
     * @param firstMessage 最初のエラーメッセージ
     * @param errorMessages エラーメッセージのリスト
     * @return ResponseEntity<ErrorResponse>
     */
    public static ResponseEntity<ErrorResponse> createValidationError(
            String firstMessage, 
            List<String> errorMessages) {
        return create(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", firstMessage, errorMessages);
    }
    
    /**
     * 認証エラーレスポンスを生成
     * 
     * @return ResponseEntity<ErrorResponse>
     */
    public static ResponseEntity<ErrorResponse> createAuthenticationError() {
        return create(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_ERROR", "認証に失敗しました");
    }
    
    /**
     * 認可エラーレスポンスを生成
     * 
     * @return ResponseEntity<ErrorResponse>
     */
    public static ResponseEntity<ErrorResponse> createAccessDeniedError() {
        return create(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "このリソースにアクセスする権限がありません");
    }
    
    /**
     * リソース未検出エラーレスポンスを生成
     * 
     * @return ResponseEntity<ErrorResponse>
     */
    public static ResponseEntity<ErrorResponse> createNotFoundError() {
        return create(HttpStatus.NOT_FOUND, "NOT_FOUND", "リソースが見つかりません");
    }
    
    /**
     * システムエラーレスポンスを生成
     * 
     * @param errorCode エラーコード
     * @param message エラーメッセージ
     * @return ResponseEntity<ErrorResponse>
     */
    public static ResponseEntity<ErrorResponse> createSystemError(String errorCode, String message) {
        return create(HttpStatus.INTERNAL_SERVER_ERROR, errorCode, message);
    }
}
