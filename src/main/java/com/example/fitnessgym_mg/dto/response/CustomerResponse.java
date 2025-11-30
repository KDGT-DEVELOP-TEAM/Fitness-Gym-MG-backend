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
	private int age; // 年齢
	private LocalDateTime createdAt;

	// 編集モーダル用にエンティティの全フィールドを含めることもできますが、
	// ユーザー一覧に倣い、必要最小限のデータ転送とします。

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
		r.setAge(age);
		r.setCreatedAt(c.getCreatedAt());
		return r;
	}
}