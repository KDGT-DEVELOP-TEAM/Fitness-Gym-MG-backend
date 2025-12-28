package com.example.fitnessgym_mg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 認証エラーが発生した場合にスローされる例外
 * HTTPステータスコード401 Unauthorizedに対応
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthenticationException extends RuntimeException {
    
    private final String email;
    private final String requestInfo;
    
    public AuthenticationException(String message) {
        super(message);
        this.email = null;
        this.requestInfo = null;
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
        this.email = null;
        this.requestInfo = null;
    }
    
    /**
     * メールアドレスとリクエスト情報を保持するコンストラクタ
     * セキュリティログや監査の観点で、どのメールアドレスやリクエストが認証に失敗したかをトレース可能にする
     * 
     * @param email 認証を試みたメールアドレス（null許容）
     * @param requestInfo リクエスト情報（例: IPアドレス、User-Agentなど）
     */
    public AuthenticationException(String email, String requestInfo) {
        super("Authentication failed for email: " + (email != null ? email : "unknown") + 
              (requestInfo != null ? ", request: " + requestInfo : ""));
        this.email = email;
        this.requestInfo = requestInfo;
    }
    
    /**
     * メールアドレスを取得
     * 
     * @return メールアドレス（設定されていない場合はnull）
     */
    public String getEmail() {
        return email;
    }
    
    /**
     * リクエスト情報を取得
     * 
     * @return リクエスト情報（設定されていない場合はnull）
     */
    public String getRequestInfo() {
        return requestInfo;
    }
}
