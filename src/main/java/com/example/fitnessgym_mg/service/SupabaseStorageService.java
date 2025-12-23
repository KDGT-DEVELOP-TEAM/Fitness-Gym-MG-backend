package com.example.fitnessgym_mg.service;

import com.example.fitnessgym_mg.config.SupabaseStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseStorageService {
    
    private final SupabaseStorageProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    
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
            headers.setContentType(MediaType.parseMediaType(file.getContentType()));
            
            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully uploaded file: {}", storageKey);
                return storageKey;
            }
            
            throw new RuntimeException("Failed to upload file: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Error uploading file to Supabase Storage: {}", storageKey, e);
            throw new RuntimeException("Storage upload failed", e);
        }
    }
    
    /**
     * Supabase Storageからファイルを削除
     * @param storageKey Storage内のパス
     */
    public void deleteFile(String storageKey) {
        try {
            String url = String.format("%s/storage/v1/object/%s/%s",
                properties.getUrl(), properties.getBucket(), storageKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + properties.getServiceKey());
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.info("Successfully deleted file: {}", storageKey);
        } catch (Exception e) {
            log.error("Error deleting file from Supabase Storage: {}", storageKey, e);
            // 削除エラーはログに記録するが、例外をスローしない（既に削除済みの可能性）
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
            
            throw new RuntimeException("Failed to generate signed URL");
        } catch (Exception e) {
            log.error("Error generating signed URL: {}", storageKey, e);
            throw new RuntimeException("Signed URL generation failed", e);
        }
    }
}
