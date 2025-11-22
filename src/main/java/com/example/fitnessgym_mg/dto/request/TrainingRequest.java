package com.example.fitnessgym_mg.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingRequest {

    @NotNull(message = "順序は必須です")
    @Positive(message = "順序は1以上である必要があります")
    private Integer orderNo;

    @NotBlank(message = "トレーニング名称は必須です")
    private String name;

    @NotNull(message = "回数は必須です")
    @Positive(message = "回数は1以上である必要があります")
    private Integer reps;
}
