package com.example.fitnessgym_mg.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * エンティティが見つからない場合にスローされる例外
 * HTTPステータスコード404 Not Foundに対応
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EntityNotFoundException extends RuntimeException {
    
    private final String entityType;
    private final String entityId;
    
    public EntityNotFoundException(String message) {
        super(message);
        this.entityType = null;
        this.entityId = null;
    }
    
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.entityType = null;
        this.entityId = null;
    }
    
    /**
     * エンティティタイプとIDを保持するコンストラクタ
     * ログ解析やデバッグの観点で、どのエンティティが見つからなかったかをトレース可能にする
     * 
     * @param entityType エンティティタイプ（例: "Customer", "User", "Lesson"）
     * @param entityId エンティティID（UUIDの文字列表現など）
     */
    public EntityNotFoundException(String entityType, String entityId) {
        super(entityType + " が見つかりません。ID: " + entityId);
        this.entityType = entityType;
        this.entityId = entityId;
    }
    
    /**
     * エンティティタイプを取得
     * 
     * @return エンティティタイプ（設定されていない場合はnull）
     */
    public String getEntityType() {
        return entityType;
    }
    
    /**
     * エンティティIDを取得
     * 
     * @return エンティティID（設定されていない場合はnull）
     */
    public String getEntityId() {
        return entityId;
    }
}
