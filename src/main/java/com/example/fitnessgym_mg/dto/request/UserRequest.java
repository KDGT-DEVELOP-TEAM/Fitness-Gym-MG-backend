package com.example.fitnessgym_mg.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.example.fitnessgym_mg.entity.User.UserRole;

import lombok.Data;

@Data
public class UserRequest {

	@Email
	@NotBlank
	private String email;

	@NotBlank
	@Size(min = 2, max = 50)
	private String name;

	@NotBlank
	@Size(min = 2, max = 50)
	private String kana;

	@NotBlank
	@Size(min = 8, max = 16)
	private String pass; // 編集時は null の可能性あり

	@NotNull
	private UserRole role;

	private boolean active = true;
}