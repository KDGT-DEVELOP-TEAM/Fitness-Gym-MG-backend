package com.example.fitnessgym_mg.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posture_group")
@Data
public class PostureGroup {

	@Id
	@GeneratedValue
	private Long id;

	private String name;
}
