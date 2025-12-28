package com.example.fitnessgym_mg.dto.request;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.example.fitnessgym_mg.entity.enums.UserRole;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "pass") // セキュリティ: パスワードをログに出力しない
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

	/**
	 * パスワード
	 * 
	 * <p>新規作成時は必須です。編集時は任意です。</p>
	 * <p>仕様:</p>
	 * <ul>
	 *   <li>nullまたは空文字の場合: パスワードを変更しない（既存のパスワードを維持）</li>
	 *   <li>値が指定された場合: 8文字以上16文字以内である必要があります</li>
	 * </ul>
	 * <p>Service層では、nullまたは空文字の場合はパスワード更新処理をスキップします。</p>
	 */
	@Pattern(regexp = "^$|.{8,16}", message = "パスワードは8文字以上16文字以内で設定してください。空文字の場合は変更されません。")
	private String pass;

	@NotNull(message = "ロール選択は必須です")
	private UserRole role;

	private Set<UUID> storeIds; // MANAGERの場合にのみService側でバリデーションする

	private boolean active = true;
}