package com.example.fitnessgym_mg.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "password") // セキュリティ: パスワードをログに出力しない
public class LoginRequest {

	@NotBlank(message = "メールアドレスは必須です")
	@Email(message = "有効なメールアドレスを入力してください")
	@Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
	private String email;

	/**
	 * パスワード
	 * 
	 * <p>セキュリティ要件:</p>
	 * <ul>
	 *   <li>8文字以上</li>
	 *   <li>英字（大文字・小文字）と数字を含む必要があります</li>
	 * </ul>
	 */
	@NotBlank(message = "パスワードは必須です")
	@Size(min = 8, message = "パスワードは8文字以上で入力してください")
	@Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "パスワードは英字と数字を含めてください")
	private String password;
}