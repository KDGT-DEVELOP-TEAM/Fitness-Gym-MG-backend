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
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", firstMessage, errorMessages));
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
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", firstMessage, errorMessages));
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
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
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
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("BUSINESS_RULE_VIOLATION", e.getMessage()));
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
     * 
     * <p>パフォーマンス考慮: ビジネスロジックエラーは意図的な例外のため、
     * スタックトレースは出力しません。</p>
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        // 意図的な例外のため、スタックトレースは出力しない
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
     * 顧客が論理削除（退会済み）されている場合のハンドリング
     * 顧客が退会済みの場合に使用
     * HTTPステータスコード403 Forbiddenを返す
     */
    @ExceptionHandler(CustomerDeletedException.class)
    public ResponseEntity<ErrorResponse> handleCustomerDeleted(CustomerDeletedException e) {
        log.warn("Customer deleted: customerId={}", e.getCustomerId());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("CUSTOMER_DELETED", "顧客は退会済みです"));
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
     * 
     * <p>パフォーマンス考慮: リソース競合は意図的な例外のため、
     * スタックトレースは出力しません。</p>
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        // 意図的な例外のため、スタックトレースは出力しない
        log.warn("Resource conflict: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICT", e.getMessage()));
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
     * システム内部エラーのハンドリング
     * Service層で発生したシステムエラー（API呼び出し失敗、データベースエラーなど）を処理
     */
    @ExceptionHandler(SystemException.class)
    public ResponseEntity<ErrorResponse> handleSystemException(SystemException e) {
        log.error("System error: {}", e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("SYSTEM_ERROR", "システムエラーが発生しました"));
    }

    /**
     * 設定エラーのハンドリング
     * アプリケーションの設定が不正または不足している場合に使用
     */
    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleConfigurationException(ConfigurationException e) {
        log.error("Configuration error: {}", e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("CONFIGURATION_ERROR", "設定エラーが発生しました"));
    }

    /**
     * 実装エラーのハンドリング
     * コードの実装上の問題（インターフェースの実装不足など）を処理
     * 通常、この例外は開発時に発見され、本番環境では発生しないはずです
     */
    @ExceptionHandler(ImplementationException.class)
    public ResponseEntity<ErrorResponse> handleImplementationException(ImplementationException e) {
        log.error("Implementation error: {}", e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("IMPLEMENTATION_ERROR", "実装エラーが発生しました"));
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
        // RuntimeExceptionの場合は、より具体的な例外として扱う
        if (e instanceof RuntimeException) {
            log.error("Unexpected runtime error: {}", e.getMessage(), e);
        } else {
            // チェック例外（Checked Exception）の場合は、システムエラーとして扱う
            log.error("Unexpected checked exception: {}", e.getMessage(), e);
        }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An internal error occurred"));
    }
    
    /**
     * メールアドレスの機密情報をマスク
     * 
     * <p>セキュリティ強化: ローカル部分（@の前）とドメイン部分（@の後）の両方を部分的にマスクします。
     * これにより、メールアドレスの完全な露出を防ぎます。</p>
     * 
     * <p>マスク方法:</p>
     * <ul>
     *   <li>ローカル部分: 最初の2文字のみ表示、残りは***</li>
     *   <li>ドメイン部分: 最初のドメイン名の最初の2文字のみ表示、残りは***</li>
     * </ul>
     * 
     * <p>例:</p>
     * <ul>
     *   <li>"user@example.com" → "us***@ex***.com"</li>
     *   <li>"ab@test.co.jp" → "ab***@te***.co.jp"</li>
     * </ul>
     * 
     * @param email マスクするメールアドレス
     * @return マスクされたメールアドレス
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email;
        }
        
        String localPart = parts[0];
        String domain = parts[1];
        
        // ローカル部分のマスク（最初の2文字のみ表示、残りは***）
        String maskedLocal;
        if (localPart.length() <= 2) {
            maskedLocal = "***";
        } else {
            maskedLocal = localPart.substring(0, 2) + "***";
        }
        
        // ドメイン部分のマスク（最初のドメイン名の最初の2文字のみ表示）
        int dotIndex = domain.indexOf('.');
        String maskedDomain;
        if (dotIndex > 0) {
            // ドメイン名の最初の2文字のみ表示
            String domainName = domain.substring(0, dotIndex);
            String domainSuffix = domain.substring(dotIndex); // .com, .co.jp など
            String maskedDomainName = domainName.length() <= 2 
                ? "***" 
                : domainName.substring(0, 2) + "***";
            maskedDomain = maskedDomainName + domainSuffix;
        } else {
            // ドットがない場合は、全体をマスク
            maskedDomain = domain.length() <= 2 ? "***" : domain.substring(0, 2) + "***";
        }
        
        return maskedLocal + "@" + maskedDomain;
    }
}
