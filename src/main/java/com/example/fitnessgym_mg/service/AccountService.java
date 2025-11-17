package com.example.fitnessgym_mg.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.request.UserRequest;
import com.example.fitnessgym_mg.dto.response.UserResponse;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	// --- keyword + role + sort の総合検索 ---
	@Transactional(readOnly = true)
	public List<UserResponse> searchUsers(String keyword, String role, String sort) {

		// 1. ソートオブジェクトの生成
		Sort sortObj;
		switch (sort) {
		case "kana": // 50音順（kana を基準）
			sortObj = Sort.by("kana").ascending(); // 昇順
			break;
		case "created": // 新着順 (created_at を基準)
		default:
			sortObj = Sort.by("createdAt").descending(); // 降順
			break;
		}

		// 2. 検索メソッドの選択と実行
		List<User> users;

		// 役割（role）が指定されている場合
		if (role != null && !role.isEmpty()) {
			// ロールとキーワードの両方で検索するカスタムメソッドを使用
			users = userRepository.findByNameContainingIgnoreCaseOrKanaContainingIgnoreCaseAndRole(
					keyword, keyword, role, sortObj);

			// キーワードのみ指定されている場合
		} else if (keyword != null && !keyword.isEmpty()) {
			// キーワードのみで検索するカスタムメソッドを使用
			users = userRepository.findByNameContainingIgnoreCaseOrKanaContainingIgnoreCase(
					keyword, keyword, sortObj);

			// 検索条件がない場合（全件取得）
		} else {
			// findAll(Sort) を使用
			users = userRepository.findAll(sortObj);
		}

		// 3. マッピング
		return users.stream()
				.map(UserResponse::fromEntity)
				.toList();
	}

	// ユーザー作成
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

	// 更新
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

	// 有効/無効
	public void toggleActive(UUID id) {
		User user = userRepository.findById(id)
				.orElseThrow();

		user.setActive(!user.isActive());
		userRepository.save(user);
	}

	// 削除
	public void delete(UUID id) {
		User u = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found"));

		// 有効ユーザーは削除不可
		if (u.isActive()) {
			throw new IllegalStateException("有効ユーザーは削除できません");
		}

		userRepository.delete(u);
	}
}
