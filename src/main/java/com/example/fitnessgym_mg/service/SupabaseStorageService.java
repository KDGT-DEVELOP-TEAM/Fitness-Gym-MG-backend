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
     * @return 署名付きURL
     */
    public String generateSignedUrl(String storageKey, int expiresInSeconds) {
        try {
            String url = String.format("%s/storage/v1/object/sign/%s/%s",
                properties.getUrl(), properties.getBucket(), storageKey);
            
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
                log.info("Successfully generated signed URL for: {}", storageKey);
                return signedUrl;
            }
            
            throw new StorageException("Failed to generate signed URL");
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating signed URL: {}", storageKey, e);
            throw new StorageException("Signed URL generation failed", e);
        }
    }
}
