package com.example.fitnessgym_mg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 認証エラーが発生した場合にスローされる例外
 * HTTPステータスコード401 Unauthorizedに対応
 * 
 * <p>注意: {@code @ResponseStatus}アノテーションは、{@link GlobalExceptionHandler}が
 * 設定されていない場合のフォールバックとして機能します。
 * 通常は{@link GlobalExceptionHandler}がHTTPステータスコードを設定するため、
 * このアノテーションは実質的に使用されませんが、明示的なドキュメントとして残しています。</p>
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
     * <p>セキュリティ考慮: 例外メッセージにはマスク済みのメールアドレスを含めます。
     * これにより、スタックトレースやログに完全なメールアドレスが露出することを防ぎます。</p>
     * 
     * @param email 認証を試みたメールアドレス（null許容）
     * @param requestInfo リクエスト情報（例: IPアドレス、User-Agentなど）
     */
    public AuthenticationException(String email, String requestInfo) {
        // セキュリティ: 例外メッセージにはマスク済みのメールアドレスを含める
        // GlobalExceptionHandlerのmaskEmail()と同じロジックを適用
        // 注意: super()の前にメソッド呼び出しはできないため、staticメソッドとして実装するか、
        // またはヘルパーメソッドを呼び出す前にメッセージを構築する必要がある
        super(buildMessage(email, requestInfo));
        this.email = email;
        this.requestInfo = requestInfo;
    }
    
    /**
     * 例外メッセージを構築（コンストラクタから呼び出し可能なstaticメソッド）
     * 
     * <p>セキュリティ: メールアドレスはSecurityUtil.maskEmail()を使用してマスクします。</p>
     * 
     * @param email 認証を試みたメールアドレス（null許容）
     * @param requestInfo リクエスト情報（例: IPアドレス、User-Agentなど）
     * @return 構築された例外メッセージ
     */
    private static String buildMessage(String email, String requestInfo) {
        String maskedEmail = com.example.fitnessgym_mg.util.SecurityUtil.maskEmail(email);
        return "Authentication failed for email: " + (maskedEmail != null ? maskedEmail : "unknown") + 
               (requestInfo != null ? ", request: " + requestInfo : "");
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
