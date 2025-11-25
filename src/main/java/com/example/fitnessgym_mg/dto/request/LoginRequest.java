package com.example.fitnessgym_mg.dto.request;

public class LoginRequest {
	private String email;
	private String password;

	// getter
	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}

	// setter
	public void setEmail(String email) {
		this.email = email;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
