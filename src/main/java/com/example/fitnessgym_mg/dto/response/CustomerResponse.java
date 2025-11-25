package com.example.fitnessgym_mg.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
	private Long id;
	private String name;
	private Double weight;
	private Double bmi;
}