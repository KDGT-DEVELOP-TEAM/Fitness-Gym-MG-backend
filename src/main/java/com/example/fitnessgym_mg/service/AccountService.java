package com.example.fitnessgym_mg.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.fitnessgym_mg.dto.request.UserRequest;
import com.example.fitnessgym_mg.dto.response.UserResponse;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.User.UserRole;
import com.example.fitnessgym_mg.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	// --- keyword + role + sort の総合検索 ---
	public List<UserResponse> searchUsers(String keyword, String role, String sort) {

		List<User> users = userRepository.findAll();

		// --- キーワード（name + kana） ---
		if (keyword != null && !keyword.isEmpty()) {
			String lower = keyword.toLowerCase();

			users = users.stream()
					.filter(u -> u.getName().toLowerCase().contains(lower) ||
							u.getKana().toLowerCase().contains(lower))
					.toList();
		}

		// --- role 絞り込み（Enum で比較） ---
		if (role != null && !role.isEmpty()) {
			try {
				UserRole roleEnum = UserRole.valueOf(role);

				users = users.stream()
						.filter(u -> u.getRole() == roleEnum)
						.toList();

			} catch (IllegalArgumentException ignored) {
				// 無効な role の時は無視
			}
		}

		// --- 並び順 ---
		switch (sort) {

		case "kana": // 50音順（kana を基準）
			users = users.stream()
					.sorted(Comparator.comparing(User::getKana))
					.toList();
			break;

		case "created": // 新着順
		default:
			users = users.stream()
					.sorted(Comparator.comparing(User::getCreatedAt).reversed())
					.toList();
			break;
		}

		return users.stream()
				.map(UserResponse::fromEntity)
				.toList();
	}

	/**
	 * --- ユーザー作成 ---
	 */
	public void create(UserRequest req) {
		User user = new User();

		user.setEmail(req.getEmail());
		user.setName(req.getName());
		user.setKana(req.getKana());
		user.setRole(req.getRole());
		user.setActive(req.isActive());
		user.setPass(passwordEncoder.encode(req.getPass()));

		userRepository.save(user);
	}

	/**
	 * --- ユーザー更新 ---
	 */
	public void update(UUID id, UserRequest req) {
		User user = userRepository.findById(id)
				.orElseThrow();

		user.setEmail(req.getEmail());
		user.setName(req.getName());
		user.setKana(req.getKana());
		user.setRole(req.getRole());
		user.setActive(req.isActive());

		// パスワードは変更時のみ更新
		if (req.getPass() != null && !req.getPass().isEmpty()) {
			user.setPass(passwordEncoder.encode(req.getPass()));
		}

		userRepository.save(user);
	}

	public void toggleActive(UUID id) {
		User user = userRepository.findById(id)
				.orElseThrow();

		user.setActive(!user.isActive());
		userRepository.save(user);
	}

	public void delete(UUID id) {
		userRepository.deleteById(id);
	}
}
