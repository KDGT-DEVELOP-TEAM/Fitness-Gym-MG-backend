package com.example.fitnessgym_mg.dto.request;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.example.fitnessgym_mg.entity.User.UserRole;

import lombok.Data;

@Data
public class UserRequest {

	@Email(message = "有効なメールアドレスを入力してください")
	@NotBlank(message = "メールアドレスは必須です")
	@Size(max = 255)
	private String email;

	@NotBlank(message = "名前は必須です")
	@Size(min = 2, max = 50)
	private String name;

	@NotBlank(message = "フリガナは必須です")
	@Size(min = 2, max = 50)
	private String kana;

	// @NotBlank は削除 (編集時の null/空文字を許可するため)
	@Size(min = 8, max = 16, message = "パスワードは8文字以上16文字以内で設定してください")
	private String pass; // 編集時は null の可能性あり

	@NotNull(message = "ロール選択は必須です")
	private UserRole role;

	private Set<UUID> storeIds; // MANAGERの場合にのみService側でバリデーションする

	private boolean active = true;
}