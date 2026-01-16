package com.example.fitnessgym_mg.controller.api;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fitnessgym_mg.dto.request.UserRequest;
import com.example.fitnessgym_mg.dto.response.UserResponse;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import com.example.fitnessgym_mg.entity.enums.UserSortType;
import com.example.fitnessgym_mg.service.AccountAuthorizationService;
import com.example.fitnessgym_mg.service.AccountService;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ユーザーアカウント管理REST APIコントローラー
 * Reactフロントエンドとの連携用
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserApiController {

	private final AccountService accountService;
	private final SecurityUtil securityUtil;
	private final AccountAuthorizationService accountAuthorizationService;

	/**
	 * GET /api/users
	 * ユーザー一覧取得（オプション選択用）
	 * 認証済みユーザー全員がアクセス可能
	 * ページングなしで全ユーザーを返す
	 */
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TRAINER')")
	@GetMapping("/users")
	public ResponseEntity<List<UserResponse>> getUsersForOptions() {
		log.debug("ユーザー一覧取得（オプション選択用）リクエスト");
		List<UserResponse> users = accountService.getAllUsersForOptions();
		log.info("ユーザー一覧取得（オプション選択用）成功: count={}", users.size());
		return ResponseEntity.ok(users);
	}

	/**
	 * GET /api/admin/users
	 * 全ユーザーアカウントの一覧取得（検索/フィルタリング可）
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/admin/users")
	public ResponseEntity<Page<UserResponse>> getUsers(
			@RequestParam(required = false) @jakarta.validation.constraints.Size(max = 100, message = "Keyword must be less than 100 characters") String name,
			@RequestParam(required = false) UserRole role,
			@RequestParam(defaultValue = "created") String sort,
			@RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(value = 0, message = "Page must be 0 or greater") int page,
			@RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(value = 1, message = "Size must be at least 1") @jakarta.validation.constraints.Max(value = 100, message = "Size must not exceed 100") int size) {

		Pageable pageable = PageRequest.of(page, size);
		UserSortType sortEnum = UserSortType.fromCode(sort);
		Page<UserResponse> userPage = accountService.searchUsers(name, role, sortEnum, null, pageable);
		log.debug("ユーザー一覧取得成功: page={}, size={}, total={}", page, size, userPage.getTotalElements());
		return ResponseEntity.ok(userPage);
	}

	/**
	 * GET /api/admin/users?name={keyword}
	 * ユーザー名での検索（上記のgetUsersメソッドでnameパラメータとして処理）
	 */

	/**
	 * GET /api/admin/users/{user_id}
	 * 特定ユーザー情報の取得
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/admin/users/{user_id}")
	public ResponseEntity<UserResponse> getUser(@PathVariable("user_id") UUID userId) {
		UserResponse user = accountService.findById(userId, null);
		return ResponseEntity.ok(user);
	}

	/**
	 * POST /api/admin/users
	 * ユーザーアカウントの新規作成
	 */
	@PreAuthorize("hasRole('ADMIN') and @accountAuthorizationService.canCreateUser(authentication, null)")
	@PostMapping("/admin/users")
	public ResponseEntity<Void> createUser(@Valid @RequestBody UserRequest request) {
		// ビジネスルールチェック（Service層で実施）
		User currentUser = securityUtil.getCurrentUserOrThrow();
		accountAuthorizationService.validateRoleChange(currentUser, null, request.getRole());
		accountAuthorizationService.checkManagerPermission(currentUser, request.getRole());

		accountService.createByAdmin(request,
				request.getStoreIds() != null ? request.getStoreIds() : Collections.emptySet());
		log.info("ユーザー作成成功: role={}", request.getRole());
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	/**
	 * PATCH /api/admin/users/{user_id}
	 * 既存ユーザーの情報更新
	 */
	@PreAuthorize("hasRole('ADMIN') and @accountAuthorizationService.canUpdateUser(authentication, #userId, null)")
	@PatchMapping("/admin/users/{user_id}")
	public ResponseEntity<Void> updateUser(
			@PathVariable("user_id") UUID userId,
			@Valid @RequestBody UserRequest request) {
		// ビジネスルールチェック（Service層で実施）
		User currentUser = securityUtil.getCurrentUserOrThrow();
		// targetUserはService層で取得
		User targetUser = accountService.findUserEntityById(userId, null);
		accountAuthorizationService.validateRoleChange(currentUser, userId, request.getRole());
		accountAuthorizationService.checkManagerPermission(currentUser, targetUser.getRole(), request.getRole());

		accountService.updateByAdmin(userId, request,
				request.getStoreIds() != null ? request.getStoreIds() : Collections.emptySet());
		log.info("ユーザー更新成功: userId={}, role={}", userId, request.getRole());
		return ResponseEntity.ok().build();
	}

	/**
	 * DELETE /api/admin/manager/users/{user_id}
	 * ユーザーアカウントの削除
	 * 注意: 仕様では /api/admin/manager/users/{user_id} となっているが、/api/admin/users/{user_id} の方が適切と思われる
	 */
	@PreAuthorize("hasRole('ADMIN') and @accountAuthorizationService.canDeleteUser(authentication, #userId, null)")
	@DeleteMapping("/admin/users/{user_id}")
	public ResponseEntity<Void> deleteUser(
			@PathVariable("user_id") UUID userId) {
		accountService.delete(userId, null);
		log.info("ユーザー削除成功: userId={}", userId);
		return ResponseEntity.ok().build();
	}

	// 店長用エンドポイント

	/**
	 * GET /api/stores/{store_id}/manager/users
	 * 全ユーザーアカウントの一覧取得（検索/フィルタリング可）
	 */
	@PreAuthorize("hasRole('MANAGER') and @authorizationFacade.canAccessStore(authentication, #storeId)")
	@GetMapping("/stores/{store_id}/manager/users")
	public ResponseEntity<Page<UserResponse>> getManagerUsers(
			@PathVariable("store_id") UUID storeId,
			@RequestParam(required = false) @jakarta.validation.constraints.Size(max = 100, message = "Keyword must be less than 100 characters") String name,
			@RequestParam(required = false) UserRole role,
			@RequestParam(defaultValue = "created") String sort,
			@RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(value = 0, message = "Page must be 0 or greater") int page,
			@RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(value = 1, message = "Size must be at least 1") @jakarta.validation.constraints.Max(value = 100, message = "Size must not exceed 100") int size) {

		log.debug("getManagerUsers called: storeId={}, name={}, role={}, sort={}, page={}, size={}", storeId, name, role, sort, page, size);
		Pageable pageable = PageRequest.of(page, size);
		UserSortType sortEnum = UserSortType.fromCode(sort);
		Page<UserResponse> userPage = accountService.searchUsers(name, role, sortEnum, storeId, pageable);
		log.debug("getManagerUsers result: totalElements={}, totalPages={}, numberOfElements={}", userPage.getTotalElements(), userPage.getTotalPages(), userPage.getNumberOfElements());
		return ResponseEntity.ok(userPage);
	}

	/**
	 * GET /api/stores/{store_id}/manager/users?name={keyword}
	 * ユーザー名での検索（上記のgetManagerUsersメソッドでnameパラメータとして処理）
	 */

	/**
	 * GET /api/stores/{store_id}/manager/users/{user_id}
	 * 特定ユーザー情報の取得
	 */
	@PreAuthorize("hasRole('MANAGER') and @authorizationFacade.canAccessStore(authentication, #storeId)")
	@GetMapping("/stores/{store_id}/manager/users/{user_id}")
	public ResponseEntity<UserResponse> getManagerUser(
			@PathVariable("store_id") UUID storeId,
			@PathVariable("user_id") UUID userId) {
		UserResponse user = accountService.findById(userId, storeId);
		return ResponseEntity.ok(user);
	}

	/**
	 * POST /api/stores/{store_id}/manager/users
	 * ユーザーアカウントの新規作成
	 */
	@PreAuthorize("hasRole('MANAGER') and @authorizationFacade.canAccessStore(authentication, #storeId) and @accountAuthorizationService.canCreateUser(authentication, #storeId)")
	@PostMapping("/stores/{store_id}/manager/users")
	public ResponseEntity<Void> createManagerUser(
			@PathVariable("store_id") UUID storeId,
			@Valid @RequestBody UserRequest request) {
		// ビジネスルールチェック（Service層で実施）
		User currentUser = securityUtil.getCurrentUserOrThrow();
		accountAuthorizationService.validateRoleChange(currentUser, null, request.getRole());
		accountAuthorizationService.checkManagerPermission(currentUser, request.getRole());

		// Manager APIではリクエストボディのstoreIdsを完全に無視し、pathパラメータのstoreIdのみを使用
		accountService.createByManager(request, storeId);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	/**
	 * PATCH /api/stores/{store_id}/manager/users/{user_id}
	 * 既存ユーザーの情報更新
	 */
	@PreAuthorize("hasRole('MANAGER') and @authorizationFacade.canAccessStore(authentication, #storeId) and @accountAuthorizationService.canUpdateUser(authentication, #userId, #storeId)")
	@PatchMapping("/stores/{store_id}/manager/users/{user_id}")
	public ResponseEntity<Void> updateManagerUser(
			@PathVariable("store_id") UUID storeId,
			@PathVariable("user_id") UUID userId,
			@Valid @RequestBody UserRequest request) {
		// ビジネスルールチェック（Service層で実施）
		User currentUser = securityUtil.getCurrentUserOrThrow();
		// targetUserはService層で取得
		User targetUser = accountService.findUserEntityById(userId, storeId);
		accountAuthorizationService.validateRoleChange(currentUser, userId, request.getRole());
		accountAuthorizationService.checkManagerPermission(currentUser, targetUser.getRole(), request.getRole());

		// Manager APIではリクエストボディのstoreIdsを完全に無視し、pathパラメータのstoreIdのみを使用
		accountService.updateByManager(userId, request, storeId);
		return ResponseEntity.ok().build();
	}

	/**
	 * DELETE /api/stores/{store_id}/manager/users/{user_id}
	 * ユーザーアカウントの削除
	 */
	@PreAuthorize("hasRole('MANAGER') and @authorizationFacade.canAccessStore(authentication, #storeId) and @accountAuthorizationService.canDeleteUser(authentication, #userId, #storeId)")
	@DeleteMapping("/stores/{store_id}/manager/users/{user_id}")
	public ResponseEntity<Void> deleteManagerUser(
			@PathVariable("store_id") UUID storeId,
			@PathVariable("user_id") UUID userId) {
		accountService.delete(userId, storeId);
		return ResponseEntity.ok().build();
	}
}
