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
     */
    private Integer orderNo;
    
    /**
     * トレーニング種目名
     */
    private String name;
    
    /**
     * 実施回数
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
                .orderNo(entity.getId() != null ? entity.getId().getOrderNo() : null)
                .name(entity.getName())
                .reps(entity.getReps())
                .build();
    }
}

