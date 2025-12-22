package com.example.fitnessgym_mg.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingRequest {
    
    @NotNull(message = "順序番号は必須です")
    private Integer orderNo;
    
    @Size(max = 100, message = "トレーニング名は100文字以内で入力してください")
    private String name;
    
    @Min(value = 0, message = "回数は0以上である必要があります")
    @Max(value = 10000, message = "回数は10000以下である必要があります")
    private Integer reps;
}

