package com.example.fitnessgym_mg.config.security.service;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginRedirectService {

	private final UserRepository userRepository;

	public String resolveRedirectUrl(String email, Authentication auth) {

		if (hasRole(auth, "ROLE_ADMIN")) {
			return "/admin/lessons";
		}

		if (hasRole(auth, "ROLE_MANAGER")) {
			return getManagerRedirect(email);
		}

		if (hasRole(auth, "ROLE_TRAINER")) {
			return "/trainer/customers";
		}

		log.warn("不明な権限のため login に戻ります。 user={}", email);
		return "/login";
	}

	private boolean hasRole(Authentication auth, String role) {
		return auth.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals(role));
	}

	private String getManagerRedirect(String email) {

		User user = userRepository.findByEmailWithStores(email)
				.orElseThrow(() -> new IllegalStateException(
						"ユーザーが存在しません: " + email));

		if (user.getStores() == null || user.getStores().isEmpty()) {
			throw new IllegalStateException("店長に店舗が割り当てられていません: " + email);
		}

		Store store = user.getStores().iterator().next();
		UUID storeId = store.getId();

		return "/manager/" + storeId + "/lessons";
	}
}