package com.example.fitnessgym_mg.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class LoginRequest {

	@NotBlank(message = "メールアドレスは必須です")
	@Email(message = "有効なメールアドレスを入力してください")
	private String email;

	//    @Size(min = 8, message = "パスワードは8文字以上で入力してください.")
	@NotBlank(message = "パスワードは必須です")
	private String password;
}
