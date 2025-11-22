package com.example.fitnessgym_mg.dto.response;

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
public class TrainingResponse {
    private Integer orderNo;
    private String name;
    private Integer reps;
}

