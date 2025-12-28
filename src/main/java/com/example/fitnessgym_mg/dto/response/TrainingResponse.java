package com.example.fitnessgym_mg.dto.response;

import com.example.fitnessgym_mg.entity.Training;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * トレーニングレスポンスDTO
 * レッスン内で実施されるトレーニング種目情報をAPIレスポンスとして返す際に使用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingResponse {
    
    /**
     * 順序番号
     * 
     * <p>トレーニングの表示順序を指定します。1以上の整数値です。</p>
     * <p>このフィールドは常に非nullです。複合主キーの一部として永続化されるため、nullになることはありません。</p>
     * <p>フロントエンド側では、この値を使用してトレーニングの並び順を制御してください。</p>
     */
    private Integer orderNo;
    
    /**
     * トレーニング種目名
     * 
     * <p>トレーニングの種目名です。</p>
     * <p>このフィールドは常に非nullです。データベース制約（nullable = false）により保証されています。</p>
     */
    private String name;
    
    /**
     * 実施回数
     * 
     * <p>トレーニングの実施回数です。1以上の整数値です。</p>
     * <p>このフィールドは常に非nullです。データベース制約（nullable = false）により保証されています。</p>
     */
    private Integer reps;
    
    /**
     * TrainingエンティティからレスポンスDTOに変換
     * 
     * @param entity Trainingエンティティ
     * @return TrainingResponse
     */
    public static TrainingResponse fromEntity(Training entity) {
        if (entity == null) {
            return null;
        }
        
        return TrainingResponse.builder()
                .orderNo(entity.getOrderNo())
                .name(entity.getName())
                .reps(entity.getReps())
                .build();
    }
}

