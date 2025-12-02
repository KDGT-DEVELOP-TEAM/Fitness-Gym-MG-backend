package com.example.fitnessgym_mg.controller.web;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.UserRepository;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

/**
 * エラー画面コントローラー
 * エラー画面に権限に応じたリダイレクト先を設定
 */
@Controller
@RequiredArgsConstructor
public class ErrorController {

	private final SecurityUtil securityUtil;
	private final UserRepository userRepository;

	/**
	 * エラー画面表示（500エラーなど）
	 * 権限に応じたホームへのリダイレクト先を設定
	 */
	@GetMapping("/error")
	public String error(Model model) {
		String homeUrl = getHomeUrl();
		model.addAttribute("homeUrl", homeUrl);
		return "error/error";
	}

	/**
	 * 404エラー画面表示
	 * 権限に応じたホームへのリダイレクト先を設定
	 */
	@GetMapping("/404")
	public String error404(Model model) {
		String homeUrl = getHomeUrl();
		model.addAttribute("homeUrl", homeUrl);
		return "error/404";
	}

	/**
	 * 403エラー画面表示
	 * 権限に応じたホームへのリダイレクト先を設定
	 */
	@GetMapping("/403")
	public String error403(Model model) {
		String homeUrl = getHomeUrl();
		model.addAttribute("homeUrl", homeUrl);
		return "error/403";
	}

	/**
	 * 権限に応じたホームURLを取得
	 * 
	 * @return ホームURL
	 */
	private String getHomeUrl() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		if (authentication == null || !authentication.isAuthenticated() || 
			authentication.getPrincipal().equals("anonymousUser")) {
			return "/login";
		}

		// 権限に応じてリダイレクト先を決定
		boolean isAdmin = authentication.getAuthorities().stream()
				.anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
		boolean isManager = authentication.getAuthorities().stream()
				.anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_MANAGER"));
		boolean isTrainer = authentication.getAuthorities().stream()
				.anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_TRAINER"));

		if (isAdmin) {
			return "/admin/lessons";
		} else if (isManager) {
			// 店長の場合は所属店舗IDを取得して統計画面にリダイレクト
			try {
				String email = authentication.getName();
				User user = userRepository.findByEmailWithStores(email)
						.orElse(null);
				
				if (user != null && user.getStores() != null && !user.getStores().isEmpty()) {
					UUID storeId = user.getStores().iterator().next().getId();
					return "/manager/" + storeId + "/lessons";
				}
			} catch (Exception e) {
				// エラーが発生した場合はデフォルトのリダイレクト先を返す
			}
			return "/login";
		} else if (isTrainer) {
			return "/trainer/customers";
		}

		return "/login";
	}
}

