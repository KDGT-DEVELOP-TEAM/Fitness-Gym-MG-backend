package com.example.fitnessgym_mg.dto.response;

import java.util.UUID;

import com.example.fitnessgym_mg.entity.Store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 店舗レスポンスDTO
 * 店舗情報をAPIレスポンスとして返す際に使用
 * 
 * <p><strong>用途</strong>: プルダウン選択や一覧表示など、店舗の基本情報（IDと名前）のみが必要な場面で使用します。</p>
 * <p>含まれる情報:</p>
 * <ul>
 *   <li>店舗ID: 選択値や識別子として使用</li>
 *   <li>店舗名: 表示用</li>
 * </ul>
 * <p>詳細情報（住所、有効/無効フラグなど）が必要な場合は、別途詳細用DTOの作成を検討してください。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {
    
    /**
     * 店舗ID
     * 
     * <p>プルダウン選択や一覧表示での識別子として使用されます。</p>
     */
    private UUID id;
    
    /**
     * 店舗名
     * 
     * <p>プルダウン選択や一覧表示での表示用として使用されます。</p>
     */
    private String name;
    
    /**
     * StoreエンティティからレスポンスDTOに変換
     * 
     * @param entity Storeエンティティ
     * @return StoreResponse
     */
    public static StoreResponse fromEntity(Store entity) {
        if (entity == null) {
            return null;
        }
        
        return StoreResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}

