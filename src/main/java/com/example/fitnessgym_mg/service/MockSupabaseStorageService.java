package com.example.fitnessgym_mg.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Supabase Storage APIのモック実装（開発環境用）
 * 
 * <p>このサービスは開発環境（`!server`プロファイル）でのみ有効化されます。</p>
 * <p>本番環境では`SupabaseStorageService`が使用されます。</p>
 * 
 * <p>開発環境での動作:</p>
 * <ul>
 *   <li>ファイルアップロード: ログ出力のみ、storageKeyをそのまま返す</li>
 *   <li>ファイル削除: ログ出力のみ、常に成功（true）を返す</li>
 *   <li>署名付きURL生成: ログ出力のみ、ダミーURLを返す</li>
 * </ul>
 */
@Slf4j
@Service
@Profile("!server")
public class MockSupabaseStorageService implements StorageService {
    
    /**
     * ファイルアップロード（モック実装）
     * 開発環境では実際のアップロードは行わず、ログ出力のみ
     */
    public String uploadFile(MultipartFile file, String storageKey) {
        log.warn("MOCK: File upload skipped (development mode). storageKey={}, size={} bytes", 
            storageKey, file.getSize());
        return storageKey;
    }
    
    /**
     * ファイル削除（モック実装）
     * 開発環境では実際の削除は行わず、ログ出力のみ
     */
    public boolean deleteFile(String storageKey) {
        log.warn("MOCK: File deletion skipped (development mode). storageKey={}", storageKey);
        return true;
    }
    
    /**
     * 署名付きURL生成（モック実装）
     * 開発環境ではダミーURLを返す
     */
    public String generateSignedUrl(String storageKey, int expiresInSeconds) {
        log.warn("MOCK: Signed URL generation skipped (development mode). storageKey={}, expiresIn={}s", 
            storageKey, expiresInSeconds);
        return "https://mock-storage.example.com/" + storageKey + "?expires=" + expiresInSeconds;
    }
}

