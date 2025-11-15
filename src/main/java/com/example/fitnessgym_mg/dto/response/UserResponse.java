package com.example.fitnessgym_mg.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.User;

import lombok.Data;

@Data
public class UserResponse {

	private UUID id;
	private String email;
	private String name;
	private String kana;
	private String role;
	private boolean active;
	private LocalDateTime createdAt;

	public static UserResponse fromEntity(User u) {
		UserResponse r = new UserResponse();
		r.setId(u.getId());
		r.setName(u.getName());
		r.setKana(u.getKana());
		r.setEmail(u.getEmail());
		r.setRole(u.getRole().name());
		r.setActive(u.isActive());
		r.setCreatedAt(u.getCreatedAt());
		return r;
	}
}
