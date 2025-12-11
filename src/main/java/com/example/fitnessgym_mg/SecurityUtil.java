package com.example.fitnessgym_mg;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

	private final UserRepository userRepository;

	public Optional<User> getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated() ||
				authentication.getPrincipal().equals("anonymousUser")) {
			return Optional.empty();
		}

		String email = authentication.getName();
		return userRepository.findByEmail(email);
	}

	public boolean hasRole(String role) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null) {
			return false;
		}

		return authentication.getAuthorities().stream()
				.anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(role));
	}

	public boolean isAdmin() {
		return hasRole("ROLE_ADMIN");
	}

	public boolean isManager() {
		return hasRole("ROLE_MANAGER");
	}

	public boolean isTrainer() {
		return hasRole("ROLE_TRAINER");
	}

	public String getCurrentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}

		return authentication.getName();
	}
}