package com.example.fitnessgym_mg.service;

import com.example.fitnessgym_mg.config.SupabaseAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Supabase Auth APIとの通信を担当するサービス
 * 
 * <p><strong>セキュリティ警告:</strong></p>
 * <ul>
 *   <li>このサービスはSupabaseのService Role Keyを使用します（フル権限、RLS無視）。</li>
 *   <li>Service Role Keyが漏洩した場合、Auth全ユーザーが無制限操作可能になります。</li>
 *   <li><strong>重要:</strong> `SupabaseAuthProperties`の`serviceKey`を絶対にログに出力しないでください。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseAuthService {
    
    private final SupabaseAuthProperties properties;
    private final RestTemplate restTemplate;
    
    /**
     * Supabase Authにユーザーが存在するかチェック
     * 
     * <p>注意: このメソッドはSupabase Admin APIを使用してユーザーリストを取得し、
     * メールアドレスでフィルタリングします。大量のユーザーが存在する場合、
     * パフォーマンスに影響する可能性があります。</p>
     * 
     * @param email メールアドレス（正規化済み）
     * @return ユーザーが存在する場合true、存在しない場合false
     */
    public boolean userExists(String email) {
        if (properties.getServiceKey() == null || properties.getServiceKey().trim().isEmpty()) {
            log.error("Supabase Auth Service Key is not configured");
            return false;
        }
        
        try {
            // Supabase Admin APIでユーザーリストを取得
            // 注意: 大量のユーザーが存在する場合、パフォーマンスに影響する可能性があります
            String url = String.format("%s/auth/v1/admin/users", properties.getUrl());
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + properties.getServiceKey());
            headers.set("apikey", properties.getServiceKey());
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Object usersObj = responseBody.get("users");
                if (usersObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> users = (List<Map<String, Object>>) usersObj;
                    
                    // メールアドレスでフィルタリング（大文字小文字を区別しない）
                    boolean exists = users.stream()
                        .anyMatch(user -> {
                            Object emailObj = user.get("email");
                            if (emailObj instanceof String) {
                                return ((String) emailObj).trim().toLowerCase().equals(email.toLowerCase());
                            }
                            return false;
                        });
                    
                    log.debug("User existence check in Supabase Auth: email={}, exists={}", email, exists);
                    return exists;
                }
            }
            
            return false;
        } catch (HttpClientErrorException.NotFound e) {
            // 404はユーザーが存在しないことを意味する
            log.debug("User not found in Supabase Auth: email={}", email);
            return false;
        } catch (Exception e) {
            // エラーが発生した場合は、存在チェックに失敗したとみなす
            log.warn("Failed to check user existence in Supabase Auth: email={}, error={}", 
                email, e.getMessage());
            // エラーが発生しても、ユーザー作成を試みる（既存ユーザーの場合はSupabase側でエラーになる）
            return false;
        }
    }
    
    /**
     * Supabase Authにユーザーを作成
     * 
     * @param email メールアドレス
     * @param password パスワード（平文）
     * @return 作成されたユーザーのUUID（auth.usersテーブルのid）
     * @throws RuntimeException Supabase Auth API呼び出しが失敗した場合
     */
    public UUID createUser(String email, String password) {
        // APIキーが設定されているか確認（値は出力しない）
        if (properties.getServiceKey() == null || properties.getServiceKey().trim().isEmpty()) {
            log.error("Supabase Auth Service Key is not configured");
            throw new com.example.fitnessgym_mg.exception.ConfigurationException(
                "Supabase Auth Service Key is not configured. Please set SUPABASE_SERVICE_KEY environment variable.");
        }
        log.debug("Supabase Auth Service Key is configured (length: {})", properties.getServiceKey().length());
        
        // URLをメソッドスコープで定義（catchブロックからもアクセス可能にするため）
        String url = String.format("%s/auth/v1/admin/users", properties.getUrl());
        
        try {
            log.debug("Calling Supabase Auth API: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + properties.getServiceKey());
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", properties.getServiceKey());
            
            // パスワードの検証
            if (password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("パスワードは必須です");
            }
            if (password.length() < 6) {
                throw new IllegalArgumentException("パスワードは6文字以上である必要があります");
            }
            
            // メールアドレスの検証
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("メールアドレスは必須です");
            }
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new IllegalArgumentException("無効なメールアドレス形式です");
            }
            
            // リクエストボディ（最小限のフィールドのみ）
            Map<String, Object> body = new HashMap<>();
            body.put("email", email.trim().toLowerCase()); // メールアドレスを正規化
            body.put("password", password);
            body.put("email_confirm", true); // メール確認をスキップ
            // 注意: user_metadataとapp_metadataは、Supabase側のデータベースエラーを引き起こす可能性があるため、送信しない
            
            log.info("Supabase Auth API request: url={}, email={}, email_confirm=true", url, email);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            log.info("Sending POST request to Supabase Auth API: url={}, email={}", url, email);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            log.info("Received response from Supabase Auth API: status={}, email={}", response.getStatusCode(), email);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Object idObj = responseBody.get("id");
                
                if (idObj instanceof String) {
                    UUID authUserId = UUID.fromString((String) idObj);
                    log.info("Successfully created user in Supabase Auth: email={}, authUserId={}", email, authUserId);
                    return authUserId;
                } else if (idObj instanceof UUID) {
                    UUID authUserId = (UUID) idObj;
                    log.info("Successfully created user in Supabase Auth: email={}, authUserId={}", email, authUserId);
                    return authUserId;
                } else {
                    log.error("Unexpected id type in Supabase Auth response: {}", idObj != null ? idObj.getClass() : "null");
                    throw new com.example.fitnessgym_mg.exception.SystemException("Failed to parse auth user ID from Supabase Auth response");
                }
            }
            
            log.error("Failed to create user in Supabase Auth: status={}, body={}", 
                response.getStatusCode(), response.getBody());
            throw new com.example.fitnessgym_mg.exception.SystemException(
                "Failed to create user in Supabase Auth: " + response.getStatusCode());
            
        } catch (HttpClientErrorException.Unauthorized e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("Unauthorized error calling Supabase Auth API: email={}, status={}, url={}, response={}", 
                email, e.getStatusCode(), url, errorBody);
            throw new com.example.fitnessgym_mg.exception.ConfigurationException(
                "Supabase Auth API認証に失敗しました。Service Role Keyが正しく設定されているか確認してください。", e);
        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("HTTP client error calling Supabase Auth API: email={}, status={}, url={}, response={}", 
                email, e.getStatusCode(), url, errorBody);
            
            // 400 Bad Requestの場合、より詳細なエラーメッセージを提供
            if (e.getStatusCode().value() == 400) {
                if (errorBody != null && errorBody.contains("already registered")) {
                    throw new com.example.fitnessgym_mg.exception.ConflictException(
                        "このメールアドレスは既にSupabase Authに登録されています", e);
                }
                throw new com.example.fitnessgym_mg.exception.SystemException(
                    String.format("Supabase Auth APIリクエストが無効です: %s - %s", 
                        e.getStatusCode(), errorBody), e);
            }
            
            throw new com.example.fitnessgym_mg.exception.SystemException(
                String.format("Supabase Auth API呼び出しに失敗しました: %s - %s", e.getStatusCode(), errorBody), e);
        } catch (HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("HTTP server error calling Supabase Auth API: email={}, status={}, url={}, response={}", 
                email, e.getStatusCode(), url, errorBody);
            
            // エラーメッセージを解析して、より詳細な情報を提供
            String errorMessage = "Supabase Auth APIサーバーエラー";
            if (errorBody != null && !errorBody.isEmpty()) {
                // JSONレスポンスを解析（簡易版）
                if (errorBody.contains("\"msg\"")) {
                    // JSON形式のエラーレスポンスからメッセージを抽出
                    try {
                        // "msg":"..."の部分を抽出
                        int msgStart = errorBody.indexOf("\"msg\":\"");
                        if (msgStart >= 0) {
                            int msgEnd = errorBody.indexOf("\"", msgStart + 7);
                            if (msgEnd > msgStart) {
                                String msg = errorBody.substring(msgStart + 7, msgEnd);
                                errorMessage = String.format("Supabase Auth APIエラー: %s", msg);
                                
                                // "Database error"が含まれている場合、追加の情報を提供
                                if (msg.contains("Database error")) {
                                    errorMessage += "。Supabase側のデータベースエラーが発生しています。";
                                    errorMessage += " 考えられる原因: 既に同じメールアドレスのユーザーがSupabase Authに存在している、";
                                    errorMessage += "Customerテーブルに同じメールアドレスが登録されている、";
                                    errorMessage += "またはSupabase側のデータベース設定に問題がある可能性があります。";
                                }
                            }
                        }
                    } catch (Exception parseEx) {
                        // パースに失敗した場合は、元のエラーメッセージを使用
                        log.warn("Failed to parse error message from response: {}", errorBody);
                    }
                }
            }
            
            throw new com.example.fitnessgym_mg.exception.SystemException(errorMessage + " (詳細: " + errorBody + ")", e);
        } catch (RestClientException e) {
            log.error("Error calling Supabase Auth API to create user: email={}, url={}, error={}", 
                email, url, e.getMessage(), e);
            throw new com.example.fitnessgym_mg.exception.SystemException(
                "Supabase Auth APIへの接続に失敗しました: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error creating user in Supabase Auth: email={}, url={}, error={}", 
                email, url, e.getMessage(), e);
            throw new com.example.fitnessgym_mg.exception.SystemException(
                "Supabase Authでのユーザー作成中に予期しないエラーが発生しました", e);
        }
    }
}
