package com.example.fitnessgym_mg.dto.request;

import jakarta.validation.constraints.NotNull;
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
    
    private String name;
    
    private Integer reps;
}

