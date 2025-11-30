package com.example.fitnessgym_mg.dto.response;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.User;

import lombok.Data;

@Data
public class UserResponse {

	private UUID id;
	private String email;
	private String name;
	private String kana;
	private String role;
	private boolean active;
	private Set<UUID> storeIds;
	private LocalDateTime createdAt;

	public static UserResponse fromEntity(User u) {
		UserResponse r = new UserResponse();
		r.setId(u.getId());
		r.setName(u.getName());
		r.setKana(u.getKana());
		r.setEmail(u.getEmail());
		r.setRole(u.getRole().name());
		r.setActive(u.isActive());

		if (u.getStores() != null) {
			r.setStoreIds(u.getStores().stream()
					.map(Store::getId) // StoreエンティティからIDを抽出
					.collect(Collectors.toSet())); // Setとして格納
		} else {
			r.setStoreIds(Set.of());
		}
		// null の場合、storeId は null のまま（未選択状態）となる

		r.setCreatedAt(u.getCreatedAt());
		return r;
	}
}
