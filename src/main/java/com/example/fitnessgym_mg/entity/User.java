package com.example.fitnessgym_mg.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String email;
	private String password;
	private String role; // TRAINER, MANAGER, ADMIN
	private boolean enabled; // ←ここ

	public boolean isEnabled() {
		return enabled;
	}

	public String getRole() {
		return role;
	}

	public String getPassword() {
		return password;
	}

	// emailのgetterも必要
	public String getEmail() {
		return email;
	}

	public boolean isActive() {
		return enabled;
	}

}
