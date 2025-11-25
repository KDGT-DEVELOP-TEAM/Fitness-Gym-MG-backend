package com.example.fitnessgym_mg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.fitnessgym_mg.dto.request.LoginRequest;
import com.example.fitnessgym_mg.service.AuthService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@GetMapping("/login")
	public String showLoginForm(Model model) {
		model.addAttribute("loginRequest", new LoginRequest());
		return "auth/login"; // ← login.html のパス
	}

	@PostMapping("/login")
	public String login(@ModelAttribute LoginRequest loginRequest,
			Model model) {

		String result = authService.authenticate(loginRequest);

		switch (result) {
		case "TRAINER":
			return "redirect:/customer-selection";

		case "MANAGER":
		case "ADMIN":
			return "redirect:/statistics";

		case "INVALID_ACCOUNT":
			model.addAttribute("error", "アカウントが無効です");
			return "auth/login";

		case "WRONG_CREDENTIALS":
		default:
			model.addAttribute("error", "メールアドレスまたはパスワードが正しくありません");
			return "auth/login";
		}
	}

	@GetMapping("/forgot-password")
	public String forgotPasswordForm() {
		return "forgot-password";
	}

}
