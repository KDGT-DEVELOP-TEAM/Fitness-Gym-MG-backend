package com.example.fitnessgym_mg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 認可エラーが発生した場合にスローされる例外
 * 認証済みユーザーがリソースにアクセスする権限がない場合に使用
 * HTTPステータスコード403 Forbiddenに対応
 * 
 * <p>注意: {@code @ResponseStatus}アノテーションは、{@link GlobalExceptionHandler}が
 * 設定されていない場合のフォールバックとして機能します。
 * 通常は{@link GlobalExceptionHandler}がHTTPステータスコードを設定するため、
 * このアノテーションは実質的に使用されませんが、明示的なドキュメントとして残しています。</p>
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {
    
    private final String userId;
    private final String resource;
    
    public AccessDeniedException(String message) {
        super(message);
        this.userId = null;
        this.resource = null;
    }
    
    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
        this.userId = null;
        this.resource = null;
    }
    
    /**
     * ユーザーIDとリソース情報を保持するコンストラクタ
     * セキュリティログや監査の観点で、どのユーザーがどのリソースにアクセスできなかったかをトレース可能にする
     * 
     * @param userId アクセスを試みたユーザーID
     * @param resource アクセスしようとしたリソース（例: "customer:123", "lesson:456"）
     */
    public AccessDeniedException(String userId, String resource) {
        super("User " + userId + " is denied access to " + resource);
        this.userId = userId;
        this.resource = resource;
    }
    
    /**
     * ユーザーIDを取得
     * 
     * @return ユーザーID（設定されていない場合はnull）
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * リソース情報を取得
     * 
     * @return リソース情報（設定されていない場合はnull）
     */
    public String getResource() {
        return resource;
    }
}

