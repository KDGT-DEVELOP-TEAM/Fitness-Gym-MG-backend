package com.example.fitnessgym_mg.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.Customer;

import lombok.Data;

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
	private Customer.CustomerGender gender;
	private LocalDate birthdate; // birthdayの別名（HTMLフォームとの互換性のため）
	private String address;
	private Double height;
	private Double latestWeight; // 最新レッスンの体重（BMI計算用）

	/**
	 * Customer エンティティから CustomerResponse DTO に変換する
	 */
	public static CustomerResponse fromEntity(Customer c) {
		// 年齢計算
		int age = Period.between(c.getBirthday(), LocalDate.now()).getYears();

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
		// latestWeightは別途設定が必要（レッスンから取得）
		return r;
	}
}