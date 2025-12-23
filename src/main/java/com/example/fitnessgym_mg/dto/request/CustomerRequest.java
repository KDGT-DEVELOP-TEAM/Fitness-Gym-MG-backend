package com.example.fitnessgym_mg.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.example.fitnessgym_mg.entity.enums.Gender;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CustomerRequest {

	// ★★ 必須項目 ★★

	@NotBlank(message = "フリガナは必須です")
	@Size(max = 100)
	private String kana;

	@NotBlank(message = "名前は必須です")
	@Size(max = 100)
	private String name;

	@NotNull(message = "性別は必須です")
	private Gender gender;

	@NotNull(message = "生年月日は必須です")
	private LocalDate birthday;

	@NotNull(message = "身長は必須です")
	@Min(value = 50, message = "身長は50cm以上である必要があります")
	@Max(value = 300, message = "身長は300cm以下である必要があります")
	private BigDecimal height;

	@NotBlank(message = "メールアドレスは必須です")
	@Email(message = "有効なメールアドレスを入力してください")
	@Size(max = 255)
	private String email;

	@NotBlank(message = "電話番号は必須です")
	@Pattern(regexp = "^[0-9-]+$", message = "電話番号は数字とハイフンのみで入力してください")
	@Size(max = 12)
	private String phone;

	@NotBlank(message = "住所は必須です")
	@Size(max = 200)
	private String address;

	// ★★ 任意項目 ★★

	@Size(max = 100)
	private String medical;

	@Size(max = 100)
	private String taboo;

	@Size(max = 500)
	private String memo;

	// 初回姿勢画像ID (UUIDで送信。編集時に必須ロジックはService/Controllerで制御)
	private UUID firstPostureGroupId;

	// 有効/無効（新規作成時は必ず true で送信）
	private boolean active = true;
}