package com.example.fitnessgym_mg.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * REST API用のグローバル例外ハンドラー
 * RESTコントローラーで発生した例外を統一された形式で処理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @RequestParam のバリデーションエラーのハンドリング
     * @Validated を使用した場合に発生する ConstraintViolationException を処理
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("Validation error: {}", e.getMessage());
        // すべてのバリデーションエラーメッセージを取得
        List<String> errorMessages = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());
        // 最初のエラーメッセージをmessageフィールドに設定（後方互換性のため）
        String firstMessage = errorMessages.isEmpty() ? "Validation failed" : errorMessages.get(0);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", firstMessage, errorMessages));
    }

    /**
     * @RequestBody のバリデーションエラーのハンドリング
     * @Valid を使用した場合に発生する MethodArgumentNotValidException を処理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("Validation error: {}", e.getMessage());
        // すべてのバリデーションエラーメッセージを取得
        List<String> errorMessages = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Validation failed")
                .collect(Collectors.toList());
        // 最初のエラーメッセージをmessageフィールドに設定（後方互換性のため）
        String firstMessage = errorMessages.isEmpty() ? "Validation failed" : errorMessages.get(0);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", firstMessage, errorMessages));
    }

    /**
     * 不正なリクエストエラーのハンドリング
     * Controller層で意図的にthrowされたInvalidRequestExceptionを処理
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException e) {
        log.warn("Invalid request: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
    }

    /**
     * ビジネスルール違反エラーのハンドリング
     * Service層で意図的にthrowされたBusinessRuleViolationExceptionを処理
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(BusinessRuleViolationException e) {
        log.warn("Business rule violation: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("BUSINESS_RULE_VIOLATION", e.getMessage()));
    }

    /**
     * バリデーションエラーのハンドリング
     * バリデーションエラーはユーザーに表示しても問題ないため、詳細メッセージを返す
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Validation error: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
    }

    /**
     * 認証失敗エラーのハンドリング（BadCredentialsException）
     * 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
     * メールアドレスが存在するかどうかを判別できないようにする
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e) {
        log.warn("ログイン失敗: 認証に失敗しました");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("AUTHENTICATION_ERROR", "メールアドレスまたはパスワードが正しくありません"));
    }

    /**
     * 認証状態不整合エラーのハンドリング
     * 認証処理中に状態不整合が発生した場合に使用
     */
    @ExceptionHandler(AuthenticationStateException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationStateException(AuthenticationStateException e) {
        log.error("ログイン処理で不整合が発生しました: {}", e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("AUTHENTICATION_ERROR", "認証に失敗しました"));
    }

    /**
     * ビジネスロジックエラーのハンドリング
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        log.warn("Business logic error: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("BUSINESS_LOGIC_ERROR", e.getMessage()));
    }

    /**
     * エンティティが見つからない場合のハンドリング
     * 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e) {
        // 構造化された情報を活用してログ出力
        if (e.getEntityType() != null || e.getEntityId() != null) {
            log.warn("Entity not found: entityType={}, entityId={}", 
                e.getEntityType() != null ? e.getEntityType() : "unknown",
                e.getEntityId() != null ? e.getEntityId() : "unknown");
        } else {
            log.warn("Entity not found: {}", e.getMessage());
        }
        // 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", "リソースが見つかりません"));
    }

    /**
     * 認証エラーのハンドリング
     * 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        // 構造化された情報を活用し、機密情報をマスク
        if (e.getEmail() != null || e.getRequestInfo() != null) {
            log.warn("Authentication error: email={}, requestInfo={}", 
                e.getEmail() != null ? maskEmail(e.getEmail()) : "unknown",
                e.getRequestInfo() != null ? e.getRequestInfo() : "unknown");
        } else {
            log.warn("Authentication error: {}", e.getMessage());
        }
        // 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("AUTHENTICATION_ERROR", "認証に失敗しました"));
    }

    /**
     * 認可エラーのハンドリング
     * 認証済みユーザーがリソースにアクセスする権限がない場合に使用
     * HTTPステータスコード403 Forbiddenを返す
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        // 構造化された情報を活用してログ出力
        if (e.getUserId() != null || e.getResource() != null) {
            log.warn("Access denied: userId={}, resource={}", 
                e.getUserId() != null ? e.getUserId() : "unknown",
                e.getResource() != null ? e.getResource() : "unknown");
        } else {
            log.warn("Access denied: {}", e.getMessage());
        }
        // 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCESS_DENIED", "このリソースにアクセスする権限がありません"));
    }

    /**
     * データベース整合性違反エラーのハンドリング
     * UNIQUE制約違反の場合はConflictExceptionに変換
     * その他のDB制約違反（NOT NULL、FK制約など）は適切な例外に変換
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMessage());
        
        // HibernateのConstraintViolationExceptionを取得
        Throwable rootCause = e.getRootCause();
        if (rootCause instanceof org.hibernate.exception.ConstraintViolationException) {
            org.hibernate.exception.ConstraintViolationException cve = 
                (org.hibernate.exception.ConstraintViolationException) rootCause;
            String constraintName = cve.getConstraintName();
            
            // UNIQUE制約違反の場合はCONFLICTとして扱う
            if (constraintName != null && constraintName.toUpperCase().contains("UNIQUE")) {
                return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("CONFLICT", "リソースが既に存在します"));
            }
        }
        
        // SQLExceptionのエラーコードを確認（PostgreSQL: 23505 = UNIQUE制約違反）
        if (rootCause instanceof java.sql.SQLException) {
            java.sql.SQLException sqlEx = (java.sql.SQLException) rootCause;
            if ("23505".equals(sqlEx.getSQLState())) { // PostgreSQL UNIQUE制約違反
                return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("CONFLICT", "リソースが既に存在します"));
            }
        }
        
        // フォールバック: メッセージ文字列で判定（後方互換性のため）
        String message = e.getMessage();
        if (message != null && (message.contains("UNIQUE") || message.contains("unique") || 
            message.contains("duplicate") || message.contains("Duplicate"))) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICT", "リソースが既に存在します"));
        }
        
        // その他のDB制約違反（NOT NULL、FK制約など）はBAD_REQUESTとして扱う
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("DATA_INTEGRITY_ERROR", "データ整合性エラーが発生しました"));
    }

    /**
     * リソース競合エラーのハンドリング
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        log.warn("Resource conflict: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICT", e.getMessage()));
    }

    /**
     * ファイルサイズ超過エラーのハンドリング
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("File size exceeds maximum: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("FILE_SIZE_EXCEEDED", "File size exceeds the maximum allowed size"));
    }

    /**
     * ストレージ関連エラーのハンドリング
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageException(StorageException e) {
        log.error("Storage error: {}", e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("STORAGE_ERROR", "Storage operation failed"));
    }

    /**
     * 予期しないRuntimeExceptionのハンドリング
     * 注意: より具体的な例外ハンドラーの後に配置する必要がある
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("Runtime error: {}", e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An internal error occurred"));
    }
    
    /**
     * メールアドレスの機密情報をマスク
     * 
     * @param email マスクするメールアドレス
     * @return マスクされたメールアドレス（例: "ab***@example.com"）
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email;
        }
        if (parts[0].length() <= 2) {
            return "***@" + parts[1];
        }
        return parts[0].substring(0, 2) + "***@" + parts[1];
    }
}
