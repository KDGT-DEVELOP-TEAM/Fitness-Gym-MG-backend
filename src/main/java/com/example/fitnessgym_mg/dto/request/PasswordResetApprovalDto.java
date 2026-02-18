package com.example.fitnessgym_mg.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.ToString;

/**
 * パスワードリセット承認DTO
 */
@Data
@ToString(exclude = "newPassword") // セキュリティ: パスワードをログに出力しない
public class PasswordResetApprovalDto {

	@NotNull(message = "リクエストIDは必須です")
	private UUID requestId;

	/**
	 * 新しいパスワード
	 * 
	 * <p>セキュリティ要件:</p>
	 * <ul>
	 *   <li>8文字以上</li>
	 *   <li>英字（大文字・小文字）と数字を含む必要があります</li>
	 * </ul>
	 */
	@NotBlank(message = "新しいパスワードは必須です")
	@Size(min = 8, message = "パスワードは8文字以上で入力してください")
	@Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "パスワードは英字と数字を含めてください")
	private String newPassword;

	@Size(max = 1000, message = "メモは1000文字以内で入力してください")
	private String note;
}
