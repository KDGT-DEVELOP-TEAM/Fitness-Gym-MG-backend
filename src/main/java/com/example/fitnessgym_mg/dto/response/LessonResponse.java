package com.example.fitnessgym_mg.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {
	private LocalDateTime reservationDate;
	private double weight;
	private double bmi;
}