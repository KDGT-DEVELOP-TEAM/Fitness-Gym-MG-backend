package com.example.fitnessgym_mg.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "password") // セキュリティ: パスワードをログに出力しない
public class LoginRequest {

	@NotBlank(message = "メールアドレスは必須です")
	@Email(message = "有効なメールアドレスを入力してください")
	private String email;

	@NotBlank(message = "パスワードは必須です")
	@Size(min = 8, message = "パスワードは8文字以上で入力してください")
	private String password;
}