package com.example.fitnessgym_mg.service;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserManagementOperation;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import com.example.fitnessgym_mg.exception.AccessDeniedException;
import com.example.fitnessgym_mg.exception.BusinessRuleViolationException;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ユーザーアカウント管理に関する認可チェックを一元管理するサービス
 * 認可ロジックをService層から分離し、Controller層やその他の層から使用可能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountAuthorizationService {

	private final AuthorizationFacade authorizationFacade;
	private final SecurityUtil securityUtil;
	private final AccountService accountService;

	/**
	 * ユーザー管理の認可チェック（統合版）
	 * 
	 * <p>「誰が」「どのロールで」「どの店舗に」という軸を1箇所に集約。</p>
	 * <p>以下の判断をまとめて実施:</p>
	 * <ul>
	 *   <li>ロール制約（UserRole.canManage）</li>
	 *   <li>店舗アクセス権（CustomerAuthorizationService.canAccessStore）</li>
	 *   <li>操作種別（create/update/delete）</li>
	 * </ul>
	 * 
	 * @param currentUser 現在のユーザー
	 * @param targetUser 操作対象のユーザー（CREATEの場合はnull、UPDATE/DELETEの場合は必須）
	 * @param storeId 店舗ID（Manager用エンドポイントの場合のみ指定）
	 * @param operation 操作種別（CREATE, UPDATE, DELETE）
	 * @throws IllegalStateException UPDATE/DELETE操作でtargetUserがnullの場合（設計違反）
	 * @throws AccessDeniedException 認可不可の場合
	 * @throws BusinessRuleViolationException ビジネスルール違反の場合
	 */
	public void checkCanManageUser(User currentUser, User targetUser, UUID storeId, UserManagementOperation operation) {
		// operationに応じてtargetUserの必須性を検証
		switch (operation) {
		case UPDATE, DELETE -> {
			if (targetUser == null) {
				throw new IllegalStateException("設計違反: UPDATE/DELETE操作にはtargetUserが必須です");
			}
		}
		case CREATE -> {
			// CREATEの場合はtargetUserはnullでOK
		}
		}

		// 1. ロール制約チェック
		if (targetUser != null) {
			if (!currentUser.getRole().canManage(targetUser.getRole())) {
				throw new BusinessRuleViolationException("このロールのユーザーを管理する権限がありません");
			}
		}

		// 2. 店舗アクセス権チェック
		if (storeId != null) {
			if (!authorizationFacade.canAccessStore(currentUser, storeId)) {
				throw new AccessDeniedException("この店舗にアクセスする権限がありません");
			}
		}

		// 3. 操作種別による追加チェック（必要に応じて）
		// 例: DELETE操作の場合は追加のチェックなど
	}

	/**
	 * ユーザー作成の認可チェック
	 * 
	 * @param currentUser 現在のユーザー
	 * @param storeId 店舗ID（Manager用エンドポイントの場合のみ指定）
	 */
	public void checkCanCreateUser(User currentUser, UUID storeId) {
		checkCanManageUser(currentUser, null, storeId, UserManagementOperation.CREATE);
	}

	/**
	 * ユーザー更新の認可チェック
	 * 
	 * @param currentUser 現在のユーザー
	 * @param targetUser 操作対象のユーザー（必須）
	 * @param storeId 店舗ID（Manager用エンドポイントの場合のみ指定）
	 */
	public void checkCanUpdateUser(User currentUser, User targetUser, UUID storeId) {
		checkCanManageUser(currentUser, targetUser, storeId, UserManagementOperation.UPDATE);
	}

	/**
	 * ユーザー削除の認可チェック
	 * 
	 * @param currentUser 現在のユーザー
	 * @param targetUser 操作対象のユーザー（必須）
	 * @param storeId 店舗ID（Manager用エンドポイントの場合のみ指定）
	 */
	public void checkCanDeleteUser(User currentUser, User targetUser, UUID storeId) {
		checkCanManageUser(currentUser, targetUser, storeId, UserManagementOperation.DELETE);
	}

	/**
	 * マネージャー権限チェック
	 * マネージャーはトレーナーのみ編集可能
	 * 
	 * @param currentUser 現在のユーザー
	 * @param targetRole 対象ユーザーのロール
	 */
	public void checkManagerPermission(User currentUser, UserRole targetRole) {
		if (!currentUser.getRole().canManage(targetRole)) {
			throw new BusinessRuleViolationException("マネージャーはトレーナーのみ編集可能です。");
		}
	}

	/**
	 * マネージャー権限チェック（オーバーロード）
	 * 編集前と編集後の両方のロールをチェック
	 * 
	 * @param currentUser 現在のユーザー
	 * @param currentRole 現在のロール
	 * @param newRole 新しいロール
	 */
	public void checkManagerPermission(User currentUser, UserRole currentRole, UserRole newRole) {
		checkManagerPermission(currentUser, currentRole);
		checkManagerPermission(currentUser, newRole);
	}

	/**
	 * role変更の制約を検証
	 * 
	 * <p>以下の制約を保証:</p>
	 * <ul>
	 *   <li>自分自身のrole変更を禁止</li>
	 *   <li>ADMIN以外がADMINを作れない（create用）</li>
	 * </ul>
	 * 
	 * @param currentUser 現在のユーザー
	 * @param targetUserId 変更対象のユーザーID（update用、createの場合はnull）
	 * @param newRole 新しいロール
	 * @throws BusinessRuleViolationException 自分自身のrole変更を試みた場合
	 * @throws AccessDeniedException ADMIN以外がADMINを作成しようとした場合
	 */
	public void validateRoleChange(User currentUser, UUID targetUserId, UserRole newRole) {
		// 自分自身のrole変更を禁止（update用）
		if (targetUserId != null && currentUser.getId().equals(targetUserId)) {
			throw new BusinessRuleViolationException("自分自身のロールを変更することはできません");
		}

		// ADMIN以外がADMINを作れない（create用）
		if (newRole == UserRole.ADMIN && currentUser.getRole() != UserRole.ADMIN) {
			throw new AccessDeniedException("ADMINロールを作成できるのはADMINのみです");
		}
	}

	/**
	 * 監査ログ閲覧の認可チェック
	 * 
	 * <p>監査ログは個人情報・操作履歴・セキュリティ情報を含むため、ADMINロールのみ閲覧可能。</p>
	 * 
	 * @param currentUser 現在のユーザー
	 * @throws AccessDeniedException ADMINロールでない場合
	 */
	public void checkCanViewAuditLogs(User currentUser) {
		if (currentUser.getRole() != UserRole.ADMIN) {
			throw new AccessDeniedException("監査ログの閲覧はADMINロールのみ可能です");
		}
	}

	/**
	 * SpEL用: ユーザー作成の認可チェック
	 * 
	 * <p>@PreAuthorizeのSpELから呼び出すためのメソッド。
	 * 引数のAuthenticationから現在のユーザーを取得してcheckCanCreateUserを呼び出す。</p>
	 * 
	 * <p>SpEL用メソッドは絶対に例外を外に出さない。すべての例外をキャッチしてfalseを返す。</p>
	 * 
	 * @param authentication Spring Securityの認証情報
	 * @param storeId 店舗ID（Manager用エンドポイントの場合のみ指定）
	 * @return 認可可能な場合 true
	 */
	public boolean canCreateUser(Authentication authentication, UUID storeId) {
		try {
			// 引数のAuthenticationを正として扱う
			User currentUser = securityUtil.getUserFromAuthenticationOrThrow(authentication);
			checkCanCreateUser(currentUser, storeId);
			return true;
		} catch (com.example.fitnessgym_mg.exception.AuthenticationException
				| com.example.fitnessgym_mg.exception.AccessDeniedException
				| com.example.fitnessgym_mg.exception.BusinessRuleViolationException e) {
			// 想定内の認可失敗: ログなしでfalseを返す
			return false;
		} catch (RuntimeException e) {
			// 異常系（DB障害、NPE、設定ミス等）: ログ出力してfalseを返す
			log.error("Authorization check failed unexpectedly for createUser, storeId={}", storeId, e);
			return false;
		}
	}

	/**
	 * SpEL用: ユーザー更新の認可チェック
	 * 
	 * <p>SpEL用メソッドは絶対に例外を外に出さない。すべての例外をキャッチしてfalseを返す。</p>
	 * <p>SpELは「判断のみ」を担当。データ取得はService層で実施。</p>
	 * 
	 * @param authentication Spring Securityの認証情報
	 * @param userId 操作対象のユーザーID（必須）
	 * @param storeId 店舗ID（Manager用エンドポイントの場合のみ指定）
	 * @return 認可可能な場合 true
	 */
	public boolean canUpdateUser(Authentication authentication, UUID userId, UUID storeId) {
		try {
			// 引数のAuthenticationを正として扱う
			User currentUser = securityUtil.getUserFromAuthenticationOrThrow(authentication);
			// Service層でUserエンティティを取得
			User targetUser = accountService.findUserEntityById(userId, storeId);
			checkCanUpdateUser(currentUser, targetUser, storeId);
			return true;
		} catch (com.example.fitnessgym_mg.exception.AuthenticationException
				| com.example.fitnessgym_mg.exception.AccessDeniedException
				| com.example.fitnessgym_mg.exception.BusinessRuleViolationException
				| com.example.fitnessgym_mg.exception.EntityNotFoundException e) {
			// 想定内の認可失敗: ログなしでfalseを返す
			return false;
		} catch (RuntimeException e) {
			// 異常系（DB障害、NPE、設定ミス等）: ログ出力してfalseを返す
			log.error("Authorization check failed unexpectedly for updateUser, userId={}, storeId={}",
					userId, storeId, e);
			return false;
		}
	}

	/**
	 * SpEL用: ユーザー削除の認可チェック
	 * 
	 * <p>SpEL用メソッドは絶対に例外を外に出さない。すべての例外をキャッチしてfalseを返す。</p>
	 * <p>SpELは「判断のみ」を担当。データ取得はService層で実施。</p>
	 * 
	 * @param authentication Spring Securityの認証情報
	 * @param userId 操作対象のユーザーID（必須）
	 * @param storeId 店舗ID（Manager用エンドポイントの場合のみ指定）
	 * @return 認可可能な場合 true
	 */
	public boolean canDeleteUser(Authentication authentication, UUID userId, UUID storeId) {
		try {
			// 引数のAuthenticationを正として扱う
			User currentUser = securityUtil.getUserFromAuthenticationOrThrow(authentication);
			// Service層でUserエンティティを取得
			User targetUser = accountService.findUserEntityById(userId, storeId);
			checkCanDeleteUser(currentUser, targetUser, storeId);
			return true;
		} catch (com.example.fitnessgym_mg.exception.AuthenticationException
				| com.example.fitnessgym_mg.exception.AccessDeniedException
				| com.example.fitnessgym_mg.exception.BusinessRuleViolationException
				| com.example.fitnessgym_mg.exception.EntityNotFoundException e) {
			// 想定内の認可失敗: ログなしでfalseを返す
			return false;
		} catch (RuntimeException e) {
			// 異常系（DB障害、NPE、設定ミス等）: ログ出力してfalseを返す
			log.error("Authorization check failed unexpectedly for deleteUser, userId={}, storeId={}",
					userId, storeId, e);
			return false;
		}
	}
}
