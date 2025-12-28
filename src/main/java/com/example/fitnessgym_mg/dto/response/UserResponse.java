package com.example.fitnessgym_mg.dto.response;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;

import lombok.Data;

/**
 * ユーザーレスポンスDTO
 * ユーザー情報をAPIレスポンスとして返す際に使用
 */
@Data
public class UserResponse {

	private UUID id;
	private String email;
	private String name;
	private String kana;
	private UserRole role;
	private boolean active;
	private Set<UUID> storeIds;
	private LocalDateTime createdAt;

	public static UserResponse fromEntity(User u) {
		if (u == null) {
			return null;
		}
		
		UserResponse r = new UserResponse();
		r.setId(u.getId());
		r.setName(u.getName());
		r.setKana(u.getKana());
		r.setEmail(u.getEmail());
		r.setRole(u.getRole());
		r.setActive(u.isActive());

		if (u.getStores() != null) {
			r.setStoreIds(u.getStores().stream()
					.map(Store::getId) // StoreエンティティからIDを抽出
					.collect(Collectors.toSet())); // Setとして格納
		} else {
			// u.getStores()がnullの場合、空Set（関連なし）を設定
			r.setStoreIds(Set.of());
		}

		r.setCreatedAt(u.getCreatedAt());
		return r;
	}
}
