package com.example.fitnessgym_mg.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * パスワードリセットリクエスト作成DTO
 */
@Data
public class PasswordResetRequestCreateDto {

	@NotBlank(message = "メールアドレスは必須です")
	@Email(message = "有効なメールアドレスを入力してください")
	@Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
	private String email;

	@NotBlank(message = "名前は必須です")
	@Size(max = 100, message = "名前は100文字以内で入力してください")
	private String name;
}
