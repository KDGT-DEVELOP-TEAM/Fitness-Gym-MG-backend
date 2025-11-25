package com.example.fitnessgym_mg.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.fitnessgym_mg.dto.request.LoginRequest;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public String authenticate(LoginRequest loginRequest) {

		// メールアドレスでユーザー取得
		User user = userRepository.findByEmail(loginRequest.getEmail());

		if (user == null) {
			return "WRONG_CREDENTIALS"; // メアドなし
		}

		// パスワードチェック
		if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
			return "WRONG_CREDENTIALS"; // パスワード違う
		}

		// 無効アカウントチェック（is_active）
		if (!user.isActive()) {
			return "INVALID_ACCOUNT";
		}

		return "SUCCESS";
	}
}
