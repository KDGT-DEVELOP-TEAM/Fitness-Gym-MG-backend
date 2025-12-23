package com.example.fitnessgym_mg.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.enums.Gender;

import lombok.Data;

/**
 * 顧客レスポンスDTO
 * 顧客情報をAPIレスポンスとして返す際に使用
 * 年齢は自動計算される
 */
@Data
public class CustomerResponse {

	private UUID id;
	private String name;
	private String kana;
	private boolean active;
	private String email;
	private String phone;
	private int age; // 年齢
	private LocalDateTime createdAt;
	
	// プロフィール画面用の追加フィールド
	private Gender gender;
	private LocalDate birthdate; // birthdayの別名（HTMLフォームとの互換性のため）
	private String address;
	private BigDecimal height;
	private BigDecimal latestWeight; // 最新レッスンの体重（BMI計算用）
	private UUID firstPostureGroupId; // 初回姿勢画像ID

	/**
	 * Customer エンティティから CustomerResponse DTO に変換する
	 */
	public static CustomerResponse fromEntity(Customer c) {
		if (c == null) {
			return null;
		}
		
		// 年齢計算（nullチェック追加）
		int age = 0;
		if (c.getBirthday() != null) {
			age = Period.between(c.getBirthday(), LocalDate.now()).getYears();
		}

		CustomerResponse r = new CustomerResponse();
		r.setId(c.getId());
		r.setName(c.getName());
		r.setKana(c.getKana());
		r.setActive(c.isActive());
		r.setEmail(c.getEmail());
		r.setPhone(c.getPhone());
		r.setAge(age);
		r.setCreatedAt(c.getCreatedAt());
		r.setGender(c.getGender());
		r.setBirthdate(c.getBirthday()); // birthdayをbirthdateとして設定
		r.setAddress(c.getAddress());
		r.setHeight(c.getHeight());
		r.setFirstPostureGroupId(c.getFirstPostureGroupId());
		// latestWeightは別途設定が必要（レッスンから取得）
		return r;
	}
}