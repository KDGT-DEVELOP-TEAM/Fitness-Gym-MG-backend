package com.example.fitnessgym_mg.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.request.PasswordResetApprovalDto;
import com.example.fitnessgym_mg.dto.request.PasswordResetRejectionDto;
import com.example.fitnessgym_mg.dto.request.PasswordResetRequestCreateDto;
import com.example.fitnessgym_mg.dto.response.PasswordResetRequestResponse;
import com.example.fitnessgym_mg.entity.PasswordResetRequest;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.ActionType;
import com.example.fitnessgym_mg.entity.enums.PasswordResetStatus;
import com.example.fitnessgym_mg.entity.enums.TargetTableType;
import com.example.fitnessgym_mg.exception.BusinessRuleViolationException;
import com.example.fitnessgym_mg.exception.EntityNotFoundException;
import com.example.fitnessgym_mg.repository.PasswordResetRequestRepository;
import com.example.fitnessgym_mg.repository.UserRepository;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * パスワードリセットサービス
 * パスワードリセットリクエストの管理機能を提供
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

	private final PasswordResetRequestRepository passwordResetRequestRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AccountAuthorizationService accountAuthorizationService;
	private final AuditLogService auditLogService;
	private final SecurityUtil securityUtil;

	/**
	 * パスワードリセットリクエストを作成
	 * 
	 * <p>セキュリティ: メールアドレスと名前が一致しない場合でも、リクエストは作成されます。
	 * これは情報漏洩を防ぐための設計です。管理者画面で警告が表示されます。</p>
	 * 
	 * @param dto リクエスト作成DTO
	 * @return 作成されたリクエスト
	 */
	@Transactional
	public PasswordResetRequestResponse createRequest(PasswordResetRequestCreateDto dto) {
		log.info("Password reset request created: email={}, name={}", dto.getEmail(), dto.getName());

		// 同一メールアドレスからの連続リクエスト制限: 1時間以内に同じメールアドレスからのリクエストが存在する場合はエラー
		OffsetDateTime oneHourAgo = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
		boolean recentRequestExists = passwordResetRequestRepository.existsByEmailAndRequestedAtAfter(dto.getEmail(), oneHourAgo);
		if (recentRequestExists) {
			log.warn("Duplicate password reset request blocked: email={}, within 1 hour", dto.getEmail());
			throw new BusinessRuleViolationException("短時間に複数のリクエストは送信できません。しばらく時間をおいてから再度お試しください。");
		}

		// メールアドレスでユーザーを検索（active条件なし）
		Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());

		// リクエストを作成（ユーザーが存在しない場合でも作成）
		PasswordResetRequest request = PasswordResetRequest.builder()
				.email(dto.getEmail())
				.name(dto.getName())
				.status(PasswordResetStatus.PENDING)
				.build();

		// メールアドレスと名前が一致する場合のみユーザーを紐づけ
		if (userOpt.isPresent()) {
			User user = userOpt.get();
			if (user.getName().equals(dto.getName())) {
				request.setUser(user);
				log.debug("User matched: userId={}, email={}, name={}", 
						user.getId(), dto.getEmail(), dto.getName());
			} else {
				log.warn("Email matches but name does not: email={}, expectedName={}, providedName={}", 
						dto.getEmail(), user.getName(), dto.getName());
			}
		} else {
			log.warn("User not found for password reset request: email={}", dto.getEmail());
		}

		PasswordResetRequest savedRequest = passwordResetRequestRepository.save(request);

		// 監査ログに記録（匿名ユーザー対応メソッドを使用）
		auditLogService.recordAuditLogOptionalUser(
				ActionType.PASSWORD_RESET_REQUEST,
				TargetTableType.PASSWORD_RESET_REQUESTS,
				savedRequest.getId(),
				null); // 匿名ユーザーのためnull

		return PasswordResetRequestResponse.fromEntity(savedRequest);
	}

	/**
	 * 未処理のパスワードリセットリクエスト一覧を取得（ページネーション対応）
	 * 
	 * <p>認可: ADMINロールのみ閲覧可能。Service層で認可チェックを実施。</p>
	 * 
	 * @param pageable ページネーション情報
	 * @return 未処理リクエストのページ
	 * @throws com.example.fitnessgym_mg.exception.AccessDeniedException ADMINロールでない場合
	 */
	@Transactional(readOnly = true)
	public Page<PasswordResetRequestResponse> getPendingRequests(Pageable pageable) {
		// 認可チェック: ADMINロールのみ閲覧可能
		User currentUser = securityUtil.getCurrentUserOrThrow();
		accountAuthorizationService.checkCanManagePasswordResetRequests(currentUser);

		Page<PasswordResetRequest> requestPage = passwordResetRequestRepository
				.findByStatusOrderByRequestedAtDesc(PasswordResetStatus.PENDING, pageable);

		return requestPage.map(PasswordResetRequestResponse::fromEntity);
	}

	/**
	 * パスワードリセットリクエストを承認
	 * 
	 * <p>認可: ADMINロールのみ実行可能。Service層で認可チェックを実施。</p>
	 * 
	 * @param dto 承認DTO
	 * @throws com.example.fitnessgym_mg.exception.AccessDeniedException ADMINロールでない場合
	 * @throws EntityNotFoundException リクエストが見つからない場合
	 * @throws BusinessRuleViolationException 既に処理済みの場合、またはユーザーが見つからない場合
	 */
	@Transactional
	public void approveRequest(PasswordResetApprovalDto dto) {
		// 認可チェック: ADMINロールのみ実行可能
		User currentUser = securityUtil.getCurrentUserOrThrow();
		accountAuthorizationService.checkCanManagePasswordResetRequests(currentUser);

		// リクエストを取得
		PasswordResetRequest request = passwordResetRequestRepository.findById(dto.getRequestId())
				.orElseThrow(() -> new EntityNotFoundException("パスワードリセットリクエストが見つかりません"));

		// 既に処理済みの場合はエラー
		if (request.getStatus() != PasswordResetStatus.PENDING) {
			throw new BusinessRuleViolationException("このリクエストは既に処理済みです");
		}

		// ユーザーが紐づいていない場合はエラー
		if (request.getUser() == null) {
			throw new BusinessRuleViolationException("このリクエストに対応するユーザーが見つかりません");
		}

		User targetUser = request.getUser();

		// パスワードを更新
		targetUser.setPassword(passwordEncoder.encode(dto.getNewPassword()));
		userRepository.save(targetUser);

		// リクエストを承認済みに更新
		request.setStatus(PasswordResetStatus.APPROVED);
		request.setProcessedBy(currentUser);
		request.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
		request.setNote(dto.getNote());
		passwordResetRequestRepository.save(request);

		log.info("Password reset request approved: requestId={}, userId={}, adminId={}", 
				dto.getRequestId(), targetUser.getId(), currentUser.getId());

		// 監査ログに記録
		// 1. パスワードリセットリクエストの承認
		auditLogService.recordAuditLog(
				ActionType.PASSWORD_RESET_APPROVE,
				TargetTableType.PASSWORD_RESET_REQUESTS,
				request.getId(),
				currentUser);

		// 2. ユーザーのパスワード更新
		auditLogService.recordAuditLog(
				ActionType.UPDATE,
				TargetTableType.USERS,
				targetUser.getId(),
				currentUser);
	}

	/**
	 * パスワードリセットリクエストを拒否
	 * 
	 * <p>認可: ADMINロールのみ実行可能。Service層で認可チェックを実施。</p>
	 * 
	 * @param dto 拒否DTO
	 * @throws com.example.fitnessgym_mg.exception.AccessDeniedException ADMINロールでない場合
	 * @throws EntityNotFoundException リクエストが見つからない場合
	 * @throws BusinessRuleViolationException 既に処理済みの場合
	 */
	@Transactional
	public void rejectRequest(PasswordResetRejectionDto dto) {
		// 認可チェック: ADMINロールのみ実行可能
		User currentUser = securityUtil.getCurrentUserOrThrow();
		accountAuthorizationService.checkCanManagePasswordResetRequests(currentUser);

		// リクエストを取得
		PasswordResetRequest request = passwordResetRequestRepository.findById(dto.getRequestId())
				.orElseThrow(() -> new EntityNotFoundException("パスワードリセットリクエストが見つかりません"));

		// 既に処理済みの場合はエラー
		if (request.getStatus() != PasswordResetStatus.PENDING) {
			throw new BusinessRuleViolationException("このリクエストは既に処理済みです");
		}

		// リクエストを拒否済みに更新
		request.setStatus(PasswordResetStatus.REJECTED);
		request.setProcessedBy(currentUser);
		request.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC));
		request.setNote(dto.getNote());
		passwordResetRequestRepository.save(request);

		log.info("Password reset request rejected: requestId={}, adminId={}", 
				dto.getRequestId(), currentUser.getId());

		// 監査ログに記録
		auditLogService.recordAuditLog(
				ActionType.PASSWORD_RESET_REJECT,
				TargetTableType.PASSWORD_RESET_REQUESTS,
				request.getId(),
				currentUser);
	}

	/**
	 * パスワードリセットリクエストを削除
	 * 
	 * <p>認可: ADMINロールのみ実行可能。Service層で認可チェックを実施。</p>
	 * 
	 * @param requestId リクエストID
	 * @throws com.example.fitnessgym_mg.exception.AccessDeniedException ADMINロールでない場合
	 * @throws EntityNotFoundException リクエストが見つからない場合
	 */
	@Transactional
	public void deleteRequest(UUID requestId) {
		// 認可チェック: ADMINロールのみ実行可能
		User currentUser = securityUtil.getCurrentUserOrThrow();
		accountAuthorizationService.checkCanManagePasswordResetRequests(currentUser);

		// リクエストを取得
		PasswordResetRequest request = passwordResetRequestRepository.findById(requestId)
				.orElseThrow(() -> new EntityNotFoundException("パスワードリセットリクエストが見つかりません"));

		// 監査ログに記録（削除前に）
		auditLogService.recordAuditLog(
				ActionType.DELETE,
				TargetTableType.PASSWORD_RESET_REQUESTS,
				request.getId(),
				currentUser);

		// リクエストを削除
		passwordResetRequestRepository.delete(request);

		log.info("Password reset request deleted: requestId={}, adminId={}", 
				requestId, currentUser.getId());
	}
}
