package com.example.fitnessgym_mg.service;

import com.example.fitnessgym_mg.config.SupabaseStorageProperties;
import com.example.fitnessgym_mg.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Supabase Storage APIとの通信を担当するサービス
 * 
 * <p><strong>セキュリティ警告:</strong></p>
 * <ul>
 *   <li>このサービスはSupabaseのService Role Keyを使用します（フル権限、RLS無視）。</li>
 *   <li>Service Role Keyが漏洩した場合、Storage全オブジェクトが無制限操作可能になります。</li>
 *   <li>このサービスは本番環境（`server`プロファイル）でのみ有効化されます。</li>
 *   <li><strong>重要:</strong> `SupabaseStorageProperties`の`serviceKey`を絶対にログに出力しないでください。</li>
 *   <li>チームルールとして、`properties`オブジェクトをログ出力する際は、`serviceKey`を除外してください。</li>
 * </ul>
 * 
 * <p>プロファイル設定:</p>
 * <ul>
 *   <li>本番環境: `spring.profiles.active=server`を設定してください。</li>
 *   <li>開発環境: このサービスは無効化されます（`@Profile("server")`のため）。</li>
 * </ul>
 */
@Slf4j
@Service
@Profile("server")
@RequiredArgsConstructor
public class SupabaseStorageService implements StorageService {
    
    private final SupabaseStorageProperties properties;
    private final RestTemplate restTemplate;
    
    /**
     * storageKeyを正規化（postures/プレフィックスを削除）
     * 
     * <p>後方互換性のため、新旧どちらの形式のstorageKeyでも動作するようにする。</p>
     * <p>正規化ルール:</p>
     * <ul>
     *   <li>storageKeyがnullの場合はnullを返す</li>
     *   <li>storageKeyが"postures/"で始まる場合は、プレフィックスを削除</li>
     *   <li>それ以外の場合はそのまま返す</li>
     * </ul>
     * 
     * @param storageKey 元のstorageKey
     * @return 正規化されたstorageKey（nullの場合はnull）
     */
    private String normalizeStorageKey(String storageKey) {
        if (storageKey == null) {
            return null;
        }
        
        // 空文字列の場合はそのまま返す（防御的プログラミング）
        if (storageKey.isEmpty()) {
            return storageKey;
        }
        
        // postures/プレフィックスが含まれている場合は削除
        if (storageKey.startsWith("postures/")) {
            String normalized = storageKey.substring("postures/".length());
            // セキュリティ: 完全なパスは出力しない（最初の50文字のみ）
            String logKey = storageKey.length() > 50 ? storageKey.substring(0, 50) + "..." : storageKey;
            log.debug("Normalized storageKey: {} -> {}", logKey, normalized.length() > 50 ? normalized.substring(0, 50) + "..." : normalized);
            return normalized;
        }
        
        return storageKey;
    }
    
    /**
     * Supabase Storageにファイルをアップロード
     * @param file アップロードするファイル
     * @param storageKey Storage内のパス（例: postures/{customerId}/{groupId}/front.jpg）
     * @return アップロードされたファイルのパス
     */
    public String uploadFile(MultipartFile file, String storageKey) {
        try {
            String url = String.format("%s/storage/v1/object/%s/%s",
                properties.getUrl(), properties.getBucket(), storageKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + properties.getServiceKey());
            // Content-Typeを固定（防御層として、クライアント申告のContent-Typeに依存しない）
            // storageKeyは常に.jpgで生成されているため、IMAGE_JPEGで固定
            // 注意: PostureImageServiceで既にファイルバリデーション（Content-Type、拡張子）を実施済み
            headers.setContentType(MediaType.IMAGE_JPEG);
            
            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully uploaded file: {}", storageKey);
                return storageKey;
            }
            
            throw new StorageException("Failed to upload file: " + response.getStatusCode());
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error uploading file to Supabase Storage: {}", storageKey, e);
            throw new StorageException("Storage upload failed", e);
        }
    }
    
    /**
     * Supabase Storageからファイルを削除
     * 
     * <p>注意: 削除失敗時は例外をスローせず、`false`を返します。
     * 既に「不整合は定期ジョブで解消」という思想があるため、呼び出し側に選択権を与えます。</p>
     * 
     * @param storageKey Storage内のパス
     * @return 削除成功時 `true`、削除失敗時 `false`
     */
    public boolean deleteFile(String storageKey) {
        try {
            String url = String.format("%s/storage/v1/object/%s/%s",
                properties.getUrl(), properties.getBucket(), storageKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + properties.getServiceKey());
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.info("Successfully deleted file: {}", storageKey);
            return true;
        } catch (Exception e) {
            log.error("Error deleting file from Supabase Storage: {}", storageKey, e);
            // 削除エラーはログに記録するが、例外をスローしない（既に削除済みの可能性）
            // 呼び出し側で削除失敗を検知できるように、falseを返す
            return false;
        }
    }
    
    /**
     * 署名付きURLを生成
     * @param storageKey Storage内のパス
     * @param expiresInSeconds 有効期限（秒）
     * @return 署名付きURL（ファイルが存在しない場合はnullを返す）
     */
    public String generateSignedUrl(String storageKey, int expiresInSeconds) {
        // storageKeyを正規化（postures/プレフィックスの削除）
        String normalizedStorageKey = normalizeStorageKey(storageKey);
        
        // 正規化後のstorageKeyがnullの場合はnullを返す
        if (normalizedStorageKey == null) {
            log.warn("Storage key is null, cannot generate signed URL");
            return null;
        }
        
        try {
            String url = String.format("%s/storage/v1/object/sign/%s/%s",
                properties.getUrl(), properties.getBucket(), normalizedStorageKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + properties.getServiceKey());
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = Map.of("expiresIn", expiresInSeconds);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String signedUrl = (String) response.getBody().get("signedURL");
                
                if (signedUrl == null || signedUrl.isEmpty()) {
                    log.warn("Generated signedUrl is null or empty: storageKey={}, normalizedStorageKey={}", 
                        storageKey.length() > 50 ? storageKey.substring(0, 50) + "..." : storageKey,
                        normalizedStorageKey.length() > 50 ? normalizedStorageKey.substring(0, 50) + "..." : normalizedStorageKey);
                    return null;
                }
                
                // Supabase APIが相対パスを返す場合、完全なURLに変換
                String finalSignedUrl = signedUrl;
                if (signedUrl.startsWith("/")) {
                    // 相対パスの場合は、SupabaseのベースURLと結合して完全なURLを生成
                    // Supabase APIは `/object/sign/...` という形式を返すが、
                    // 実際のアクセスには `/storage/v1/object/sign/...` が必要
                    if (signedUrl.startsWith("/object/sign/")) {
                        // `/object/sign/` を `/storage/v1/object/sign/` に変換
                        finalSignedUrl = properties.getUrl() + "/storage/v1" + signedUrl;
                    } else {
                        // その他の相対パスの場合はそのまま結合
                        finalSignedUrl = properties.getUrl() + signedUrl;
                    }
                    log.info("Converted relative signedUrl to absolute URL: relative={}, absolutePrefix={}", 
                        signedUrl.length() > 100 ? signedUrl.substring(0, 100) + "..." : signedUrl,
                        finalSignedUrl.length() > 150 ? finalSignedUrl.substring(0, 150) + "..." : finalSignedUrl);
                } else if (signedUrl.startsWith("http://") || signedUrl.startsWith("https://")) {
                    // 既に絶対URLの場合は、`/object/sign/` を `/storage/v1/object/sign/` に変換
                    if (signedUrl.contains("/object/sign/") && !signedUrl.contains("/storage/v1/object/sign/")) {
                        finalSignedUrl = signedUrl.replace("/object/sign/", "/storage/v1/object/sign/");
                        log.info("Converted absolute signedUrl path: from={}, to={}", 
                            signedUrl.length() > 150 ? signedUrl.substring(0, 150) + "..." : signedUrl,
                            finalSignedUrl.length() > 150 ? finalSignedUrl.substring(0, 150) + "..." : finalSignedUrl);
                    }
                }
                
                // 検証ログ: signedUrlが正しく生成されたかを確認（INFOレベルで出力）
                boolean isAbsolute = finalSignedUrl.startsWith("http://") || finalSignedUrl.startsWith("https://");
                if (!isAbsolute) {
                    log.warn("Generated signedUrl is not absolute: storageKey={}, signedUrlPrefix={}", 
                        storageKey.length() > 50 ? storageKey.substring(0, 50) + "..." : storageKey,
                        finalSignedUrl.length() > 100 ? finalSignedUrl.substring(0, 100) + "..." : finalSignedUrl);
                } else {
                    log.debug("Successfully generated absolute signedUrl: storageKey={}, normalizedStorageKey={}, signedUrlPrefix={}", 
                        storageKey.length() > 50 ? storageKey.substring(0, 50) + "..." : storageKey,
                        normalizedStorageKey.length() > 50 ? normalizedStorageKey.substring(0, 50) + "..." : normalizedStorageKey,
                        finalSignedUrl.length() > 100 ? finalSignedUrl.substring(0, 100) + "..." : finalSignedUrl);
                }
                
                return finalSignedUrl;
            }
            
            throw new StorageException("Failed to generate signed URL");
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // ファイルが存在しない場合（404エラー）は警告ログを出力してnullを返す
            log.warn("File not found in Storage (404): {}. This may indicate DB-Storage inconsistency.", 
                normalizedStorageKey.length() > 100 ? normalizedStorageKey.substring(0, 100) + "..." : normalizedStorageKey);
            return null;
        } catch (org.springframework.web.client.HttpClientErrorException.BadRequest e) {
            // 400エラーのレスポンスボディを確認して、404エラーかどうかを判定
            String responseBody = e.getResponseBodyAsString();
            if (responseBody != null && (responseBody.contains("\"statusCode\":\"404\"") 
                || responseBody.contains("\"error\":\"not_found\""))) {
                // 実際には404エラーだが、Supabaseが400として返している場合
                log.warn("File not found in Storage (404 via 400): {}. This may indicate DB-Storage inconsistency.", 
                    normalizedStorageKey.length() > 100 ? normalizedStorageKey.substring(0, 100) + "..." : normalizedStorageKey);
                return null;
            }
            // その他の400エラーは例外として処理
            log.error("Bad request when generating signed URL: {}", 
                normalizedStorageKey.length() > 100 ? normalizedStorageKey.substring(0, 100) + "..." : normalizedStorageKey, e);
            throw new StorageException("Signed URL generation failed: Bad request", e);
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating signed URL: {}", 
                normalizedStorageKey.length() > 100 ? normalizedStorageKey.substring(0, 100) + "..." : normalizedStorageKey, e);
            throw new StorageException("Signed URL generation failed", e);
        }
    }
}
