package com.example.fitnessgym_mg.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * ストレージサービスのインターフェース
 * 
 * <p>このインターフェースは、Supabase Storage APIとの通信を抽象化します。</p>
 * <p>実装クラス:</p>
 * <ul>
 *   <li>{@link SupabaseStorageService}: 本番環境用（`@Profile("server")`）</li>
 *   <li>{@link MockSupabaseStorageService}: 開発環境用（`@Profile("!server")`）</li>
 * </ul>
 */
public interface StorageService {
    
    /**
     * ストレージにファイルをアップロード
     * 
     * @param file アップロードするファイル
     * @param storageKey ストレージ内のパス（例: postures/{customerId}/{groupId}/front.jpg）
     * @return アップロードされたファイルのパス
     */
    String uploadFile(MultipartFile file, String storageKey);
    
    /**
     * ストレージからファイルを削除
     * 
     * @param storageKey ストレージ内のパス
     * @return 削除成功時 `true`、削除失敗時 `false`
     */
    boolean deleteFile(String storageKey);
    
    /**
     * 署名付きURLを生成
     * 
     * @param storageKey ストレージ内のパス
     * @param expiresInSeconds 有効期限（秒）
     * @return 署名付きURL
     */
    String generateSignedUrl(String storageKey, int expiresInSeconds);
}

