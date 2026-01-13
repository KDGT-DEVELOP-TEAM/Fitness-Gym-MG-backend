package com.example.fitnessgym_mg.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
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

	/**
	 * 生年月日
	 * 
	 * <p>過去の日付のみ有効です。未来の日付は指定できません。</p>
	 */
	@NotNull(message = "生年月日は必須です")
	@Past(message = "生年月日は過去の日付である必要があります")
	private LocalDate birthday;

	/**
	 * 身長（cm）
	 * 
	 * <p>有効範囲: 50.0cm以上、300.0cm以下</p>
	 */
	@NotNull(message = "身長は必須です")
	@DecimalMin(value = "50.0", inclusive = true, message = "身長は50cm以上である必要があります")
	@DecimalMax(value = "300.0", inclusive = true, message = "身長は300cm以下である必要があります")
	private BigDecimal height;

	@NotBlank(message = "メールアドレスは必須です")
	@Email(message = "有効なメールアドレスを入力してください")
	@Size(max = 255)
	private String email;

	/**
	 * 電話番号
	 * 
	 * <p>数字とハイフンのみで入力してください。</p>
	 * <p>エンティティの制約に合わせて、最大12文字まで許可します。入力は-を含みません。</p>
	 */
	@NotBlank(message = "電話番号は必須です")
	@Pattern(regexp = "^[0-9-]+$", message = "電話番号は数字とハイフンのみで入力してください")
	@Size(max = 12, message = "電話番号は最大12文字まで入力できます")
	private String phone;

	@NotBlank(message = "住所は必須です")
	@Size(max = 500)
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

	/**
	 * 店舗ID
	 * 
	 * <p>ADMINの場合: 不要（null可）。顧客は店舗に紐づかない。</p>
	 * <p>MANAGERの場合: パス変数から取得されるため、リクエストボディでは不要。</p>
	 * <p>店舗と紐付くのはLessonであり、顧客自体は店舗に紐づかない。</p>
	 * <p>ただし、MANAGERが作成する顧客は検索・フィルタリングのため店舗と紐付ける。</p>
	 */
	private UUID storeId;
}