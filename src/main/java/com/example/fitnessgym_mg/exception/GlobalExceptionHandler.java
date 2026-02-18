package com.example.fitnessgym_mg.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import lombok.RequiredArgsConstructor;

/**
 * REST API用のグローバル例外ハンドラー
 * RESTコントローラーで発生した例外を統一された形式で処理
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    
    private final Environment environment;
    
    /**
     * デバッグモードかどうかを判定
     * 本番環境ではスタックトレースを制限し、情報漏洩を防ぐ
     * 
     * @return デバッグモードの場合 true
     */
    private boolean isDebugMode() {
        String[] activeProfiles = environment.getActiveProfiles();
        // 本番環境プロファイルが設定されている場合はデバッグモードではない
        boolean isProduction = Arrays.stream(activeProfiles)
            .anyMatch(profile -> profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"));
        
        // 本番環境の場合は常にfalse（スタックトレースを出力しない）
        if (isProduction) {
            return false;
        }
        
        // 開発環境の場合はログレベルに依存
        return log.isDebugEnabled();
    }

    /**
     * @RequestParam のバリデーションエラーのハンドリング
     * @Validated を使用した場合に発生する ConstraintViolationException を処理
     * 
     * <p>パフォーマンス考慮: バリデーションエラーは高頻度で発生する可能性があるため、
     * スタックトレースは出力しません。</p>
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        // 高頻度で発生する可能性があるため、スタックトレースは出力しない
        log.warn("Validation error: {}", e.getMessage());
        // すべてのバリデーションエラーメッセージを取得
        List<String> errorMessages = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());
        // 最初のエラーメッセージをmessageフィールドに設定（後方互換性のため）
        String firstMessage = errorMessages.isEmpty() ? "Validation failed" : errorMessages.get(0);
        return ErrorResponseFactory.createValidationError(firstMessage, errorMessages);
    }

    /**
     * @RequestBody のバリデーションエラーのハンドリング
     * @Valid を使用した場合に発生する MethodArgumentNotValidException を処理
     * 
     * <p>パフォーマンス考慮: バリデーションエラーは高頻度で発生する可能性があるため、
     * スタックトレースは出力しません。</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        // 高頻度で発生する可能性があるため、スタックトレースは出力しない
        log.warn("Validation error: {}", e.getMessage());
        // すべてのバリデーションエラーメッセージを取得
        List<String> errorMessages = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Validation failed")
                .collect(Collectors.toList());
        // 最初のエラーメッセージをmessageフィールドに設定（後方互換性のため）
        String firstMessage = errorMessages.isEmpty() ? "Validation failed" : errorMessages.get(0);
        return ErrorResponseFactory.createValidationError(firstMessage, errorMessages);
    }

    /**
     * 不正なリクエストエラーのハンドリング
     * Controller層で意図的にthrowされたInvalidRequestExceptionを処理
     * 
     * <p>パフォーマンス考慮: クライアントの入力ミスは高頻度で発生する可能性があるため、
     * スタックトレースは出力しません。</p>
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException e) {
        // 高頻度で発生する可能性があるため、スタックトレースは出力しない
        log.warn("Invalid request: {}", e.getMessage());
        return ErrorResponseFactory.create(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
    }

    /**
     * ビジネスルール違反エラーのハンドリング
     * Service層で意図的にthrowされたBusinessRuleViolationExceptionを処理
     * 
     * <p>パフォーマンス考慮: ビジネスルール違反は意図的な例外のため、
     * スタックトレースは出力しません。</p>
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(BusinessRuleViolationException e) {
        // 意図的な例外のため、スタックトレースは出力しない
        log.warn("Business rule violation: {}", e.getMessage());
        return ErrorResponseFactory.create(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", e.getMessage());
    }

    /**
     * バリデーションエラーのハンドリング
     * バリデーションエラーはユーザーに表示しても問題ないため、詳細メッセージを返す
     * 
     * <p>パフォーマンス考慮: バリデーションエラーは高頻度で発生する可能性があるため、
     * スタックトレースは出力しません。</p>
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        // 高頻度で発生する可能性があるため、スタックトレースは出力しない
        log.warn("Validation error: {}", e.getMessage());
        return ErrorResponseFactory.create(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", e.getMessage());
    }

    /**
     * 認証失敗エラーのハンドリング（BadCredentialsException）
     * 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
     * メールアドレスが存在するかどうかを判別できないようにする
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e) {
        log.warn("ログイン失敗: 認証に失敗しました");
        return ErrorResponseFactory.create(
            HttpStatus.UNAUTHORIZED, 
            "AUTHENTICATION_ERROR", 
            "メールアドレスまたはパスワードが正しくありません");
    }

    /**
     * 認証状態不整合エラーのハンドリング
     * 認証処理中に状態不整合が発生した場合に使用
     */
    @ExceptionHandler(AuthenticationStateException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationStateException(AuthenticationStateException e) {
        // デバッグモード時のみスタックトレースを出力（本番環境での情報漏洩防止）
        if (isDebugMode()) {
            log.error("ログイン処理で不整合が発生しました: {}", e.getMessage(), e);
        } else {
            log.error("ログイン処理で不整合が発生しました: {}", e.getMessage());
        }
        return ErrorResponseFactory.createAuthenticationError();
    }

    /**
     * ビジネスロジックエラーのハンドリング
     * 
     * <p>パフォーマンス考慮: ビジネスロジックエラーは意図的な例外のため、
     * スタックトレースは出力しません。</p>
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        // 意図的な例外のため、スタックトレースは出力しない
        log.warn("Business logic error: {}", e.getMessage());
        return ErrorResponseFactory.create(HttpStatus.BAD_REQUEST, "BUSINESS_LOGIC_ERROR", e.getMessage());
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
        return ErrorResponseFactory.createNotFoundError();
    }

    /**
     * 認証エラーのハンドリング
     * 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        // 構造化された情報を活用し、機密情報をマスク
        // 注意: getEmail()は既にマスク済みの値を返すため、再度マスクする必要はない
        if (e.getEmail() != null || e.getRequestInfo() != null) {
            log.warn("Authentication error: email={}, requestInfo={}", 
                e.getEmail() != null ? e.getEmail() : "unknown",
                e.getRequestInfo() != null ? e.getRequestInfo() : "unknown");
        } else {
            log.warn("Authentication error: {}", e.getMessage());
        }
        // 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
        return ErrorResponseFactory.createAuthenticationError();
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
        return ErrorResponseFactory.createAccessDeniedError();
    }

    /**
     * 顧客が論理削除（退会済み）されている場合のハンドリング
     * 顧客が退会済みの場合に使用
     * HTTPステータスコード403 Forbiddenを返す
     */
    @ExceptionHandler(CustomerDeletedException.class)
    public ResponseEntity<ErrorResponse> handleCustomerDeleted(CustomerDeletedException e) {
        log.warn("Customer deleted: customerId={}", e.getCustomerId());
        return ErrorResponseFactory.create(HttpStatus.FORBIDDEN, "CUSTOMER_DELETED", "顧客は退会済みです");
    }

    /**
     * Spring Securityの認可エラーのハンドリング
     * @PreAuthorizeアノテーションによる認可チェックで拒否された場合に発生
     * HTTPステータスコード403 Forbiddenを返す
     */
    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(org.springframework.security.authorization.AuthorizationDeniedException e) {
        // 認可拒否の詳細情報をログに記録
        // Spring Security 6.x の AuthorizationDeniedException は getMessage() のみを提供
        String message = e.getMessage() != null ? e.getMessage() : "Access Denied";
        Throwable cause = e.getCause();
        
        log.warn("Authorization denied: message={}, cause={}", 
            message,
            cause != null ? cause.getClass().getSimpleName() + ": " + cause.getMessage() : "none");
        
        // スタックトレースの最初の数行をログに記録（デバッグ用）
        if (log.isDebugEnabled()) {
            log.debug("Authorization denied stack trace:", e);
        }
        
        // 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
        return ErrorResponseFactory.createAccessDeniedError();
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
                return ErrorResponseFactory.create(HttpStatus.CONFLICT, "CONFLICT", "リソースが既に存在します");
            }
        }
        
        // SQLExceptionのエラーコードを確認（PostgreSQL: 23505 = UNIQUE制約違反）
        if (rootCause instanceof java.sql.SQLException) {
            java.sql.SQLException sqlEx = (java.sql.SQLException) rootCause;
            if ("23505".equals(sqlEx.getSQLState())) { // PostgreSQL UNIQUE制約違反
                return ErrorResponseFactory.create(HttpStatus.CONFLICT, "CONFLICT", "リソースが既に存在します");
            }
        }
        
        // フォールバック: メッセージ文字列で判定（後方互換性のため）
        String message = e.getMessage();
        if (message != null && (message.contains("UNIQUE") || message.contains("unique") || 
            message.contains("duplicate") || message.contains("Duplicate"))) {
            return ErrorResponseFactory.create(HttpStatus.CONFLICT, "CONFLICT", "リソースが既に存在します");
        }
        
        // その他のDB制約違反（NOT NULL、FK制約など）はBAD_REQUESTとして扱う
        return ErrorResponseFactory.create(HttpStatus.BAD_REQUEST, "DATA_INTEGRITY_ERROR", "データ整合性エラーが発生しました");
    }

    /**
     * リソース競合エラーのハンドリング
     * 
     * <p>パフォーマンス考慮: リソース競合は意図的な例外のため、
     * スタックトレースは出力しません。</p>
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        // 意図的な例外のため、スタックトレースは出力しない
        log.warn("Resource conflict: {}", e.getMessage());
        return ErrorResponseFactory.create(HttpStatus.CONFLICT, "CONFLICT", e.getMessage());
    }

    /**
     * ファイルサイズ超過エラーのハンドリング
     * 
     * <p>パフォーマンス考慮: ファイルサイズ超過はクライアントの入力ミスに近いため、
     * スタックトレースは出力しません。</p>
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        // クライアントの入力ミスに近いため、スタックトレースは出力しない
        log.warn("File size exceeds maximum: {}", e.getMessage());
        return ErrorResponseFactory.create(
            HttpStatus.BAD_REQUEST, 
            "FILE_SIZE_EXCEEDED", 
            "File size exceeds the maximum allowed size");
    }

    /**
     * ストレージ関連エラーのハンドリング
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageException(StorageException e) {
        // デバッグモード時のみスタックトレースを出力（本番環境での情報漏洩防止）
        if (isDebugMode()) {
            log.error("Storage error: {}", e.getMessage(), e);
        } else {
            log.error("Storage error: {}", e.getMessage());
        }
        return ErrorResponseFactory.createSystemError("STORAGE_ERROR", "Storage operation failed");
    }

    /**
     * システム内部エラーのハンドリング
     * Service層で発生したシステムエラー（API呼び出し失敗、データベースエラーなど）を処理
     */
    @ExceptionHandler(SystemException.class)
    public ResponseEntity<ErrorResponse> handleSystemException(SystemException e) {
        // デバッグモード時のみスタックトレースを出力（本番環境での情報漏洩防止）
        if (isDebugMode()) {
            log.error("System error: {}", e.getMessage(), e);
        } else {
            log.error("System error: {}", e.getMessage());
        }
        return ErrorResponseFactory.createSystemError("SYSTEM_ERROR", "システムエラーが発生しました");
    }

    /**
     * 設定エラーのハンドリング
     * アプリケーションの設定が不正または不足している場合に使用
     */
    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleConfigurationException(ConfigurationException e) {
        // デバッグモード時のみスタックトレースを出力（本番環境での情報漏洩防止）
        if (isDebugMode()) {
            log.error("Configuration error: {}", e.getMessage(), e);
        } else {
            log.error("Configuration error: {}", e.getMessage());
        }
        return ErrorResponseFactory.createSystemError("CONFIGURATION_ERROR", "設定エラーが発生しました");
    }

    /**
     * 実装エラーのハンドリング
     * コードの実装上の問題（インターフェースの実装不足など）を処理
     * 通常、この例外は開発時に発見され、本番環境では発生しないはずです
     */
    @ExceptionHandler(ImplementationException.class)
    public ResponseEntity<ErrorResponse> handleImplementationException(ImplementationException e) {
        // デバッグモード時のみスタックトレースを出力（本番環境での情報漏洩防止）
        if (isDebugMode()) {
            log.error("Implementation error: {}", e.getMessage(), e);
        } else {
            log.error("Implementation error: {}", e.getMessage());
        }
        return ErrorResponseFactory.createSystemError("IMPLEMENTATION_ERROR", "実装エラーが発生しました");
    }

    /**
     * 予期しないExceptionのハンドリング（フォールバック）
     * 
     * <p>注意: このハンドラーは、より具体的な例外ハンドラーで処理されなかった例外をキャッチします。
     * 例外ハンドラーの順序により、以下の例外は既に処理されているため、このハンドラーには到達しません：
     * - IllegalArgumentException, IllegalStateException（既にハンドリング済み）
     * - カスタム例外（InvalidRequestException、BusinessRuleViolationExceptionなど）
     * - Spring Framework例外（ConstraintViolationException、MethodArgumentNotValidExceptionなど）</p>
     * 
     * <p>このハンドラーは、予期しないシステムエラーをキャッチする最終防衛線として機能します。</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        // デバッグモード時のみスタックトレースを出力（本番環境での情報漏洩防止）
        if (isDebugMode()) {
            // RuntimeExceptionの場合は、より具体的な例外として扱う
            if (e instanceof RuntimeException) {
                log.error("Unexpected runtime error: {}", e.getMessage(), e);
            } else {
                // チェック例外（Checked Exception）の場合は、システムエラーとして扱う
                log.error("Unexpected checked exception: {}", e.getMessage(), e);
            }
        } else {
            // 本番環境ではスタックトレースを出力しない
            if (e instanceof RuntimeException) {
                log.error("Unexpected runtime error: {}", e.getMessage());
            } else {
                log.error("Unexpected checked exception: {}", e.getMessage());
            }
        }
        return ErrorResponseFactory.createSystemError("INTERNAL_ERROR", "An internal error occurred");
    }
    
}
