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
        String maskedEmail = maskEmail(email);
        super("Authentication failed for email: " + (maskedEmail != null ? maskedEmail : "unknown") + 
              (requestInfo != null ? ", request: " + requestInfo : ""));
        this.email = email;
        this.requestInfo = requestInfo;
    }
    
    /**
     * メールアドレスの機密情報をマスク（AuthenticationException内で使用）
     * 
     * <p>GlobalExceptionHandlerのmaskEmail()と同じロジックを実装します。</p>
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
            String domainName = domain.substring(0, dotIndex);
            String domainSuffix = domain.substring(dotIndex);
            String maskedDomainName = domainName.length() <= 2 
                ? "***" 
                : domainName.substring(0, 2) + "***";
            maskedDomain = maskedDomainName + domainSuffix;
        } else {
            maskedDomain = domain.length() <= 2 ? "***" : domain.substring(0, 2) + "***";
        }
        
        return maskedLocal + "@" + maskedDomain;
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
