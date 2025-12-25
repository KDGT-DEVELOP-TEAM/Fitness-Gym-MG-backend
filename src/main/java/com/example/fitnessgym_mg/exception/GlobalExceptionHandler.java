package com.example.fitnessgym_mg.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
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
        // 最初のバリデーションエラーメッセージを取得
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    /**
     * @RequestBody のバリデーションエラーのハンドリング
     * @Valid を使用した場合に発生する MethodArgumentNotValidException を処理
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("Validation error: {}", e.getMessage());
        // 最初のバリデーションエラーメッセージを取得
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message));
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
     * ビジネスロジックエラーのハンドリング
     * 認証関連のIllegalStateExceptionは別途処理する
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        // 認証関連のIllegalStateExceptionかどうかを判定
        if (e.getMessage() != null && e.getMessage().contains("ユーザー情報の取得に失敗しました")) {
            log.error("ログイン処理で不整合が発生しました: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("AUTHENTICATION_ERROR", "認証に失敗しました"));
        }
        
        // その他のIllegalStateExceptionはビジネスロジックエラーとして処理
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
        log.warn("Entity not found: {}", e.getMessage());
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
        log.warn("Authentication error: {}", e.getMessage());
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
        log.warn("Access denied: {}", e.getMessage());
        // 情報漏洩を防ぐため、詳細情報はログに記録し、クライアントには汎用的なメッセージを返す
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCESS_DENIED", "このリソースにアクセスする権限がありません"));
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
     * 注意: より具体的な例外ハンドラーの後に配置する必要がある
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("Runtime error: {}", e.getMessage(), e);
        if (e.getMessage() != null && e.getMessage().contains("Storage")) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("STORAGE_ERROR", "Storage operation failed"));
        }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An internal error occurred"));
    }
}
