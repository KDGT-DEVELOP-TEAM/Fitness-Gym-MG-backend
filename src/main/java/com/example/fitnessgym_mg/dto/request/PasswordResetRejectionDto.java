package com.example.fitnessgym_mg.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * パスワードリセット拒否DTO
 */
@Data
public class PasswordResetRejectionDto {

	@NotNull(message = "リクエストIDは必須です")
	private UUID requestId;

	@Size(max = 1000, message = "メモは1000文字以内で入力してください")
	private String note;
}
