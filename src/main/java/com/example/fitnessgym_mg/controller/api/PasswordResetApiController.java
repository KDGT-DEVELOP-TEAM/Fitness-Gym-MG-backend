package com.example.fitnessgym_mg.controller.api;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fitnessgym_mg.dto.request.PasswordResetApprovalDto;
import com.example.fitnessgym_mg.dto.request.PasswordResetRejectionDto;
import com.example.fitnessgym_mg.dto.request.PasswordResetRequestCreateDto;
import com.example.fitnessgym_mg.dto.response.PasswordResetRequestResponse;
import com.example.fitnessgym_mg.service.PasswordResetService;

import com.example.fitnessgym_mg.validation.ValidPage;
import com.example.fitnessgym_mg.validation.ValidPageSize;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * パスワードリセットREST APIコントローラー
 * Reactフロントエンドとの連携用
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/password-reset")
@RequiredArgsConstructor
public class PasswordResetApiController {

	private final PasswordResetService passwordResetService;

	/**
	 * POST /api/password-reset/request
	 * パスワードリセットリクエストの送信
	 * 
	 * <p>認証: 不要（パスワードを忘れたユーザーが使用するため）</p>
	 * 
	 * <p>セキュリティ: メールアドレスと名前が一致しない場合でも、
	 * リクエストは作成されます。これは情報漏洩を防ぐための設計です。
	 * 管理者画面で警告が表示されます。</p>
	 */
	@PostMapping("/request")
	public ResponseEntity<PasswordResetRequestResponse> createRequest(
			@Valid @RequestBody PasswordResetRequestCreateDto dto) {
		log.info("パスワードリセットリクエスト受信: email={}", dto.getEmail());
		PasswordResetRequestResponse response = passwordResetService.createRequest(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * GET /api/password-reset/requests
	 * 未処理のパスワードリセットリクエスト一覧を取得（ページネーション対応）
	 * 
	 * <p>認証: ADMINロールのみ</p>
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/requests")
	public ResponseEntity<Page<PasswordResetRequestResponse>> getRequests(
			@RequestParam(defaultValue = "0") @ValidPage int page,
			@RequestParam(defaultValue = "10") @ValidPageSize int size) {
		log.debug("パスワードリセットリクエスト一覧取得: page={}, size={}", page, size);
		
		// リクエストは時系列（リクエスト日時の降順）で表示
		Pageable pageable = PageRequest.of(
				page, 
				size, 
				Sort.by(Sort.Direction.DESC, "requestedAt")
		);
		
		Page<PasswordResetRequestResponse> requestPage = passwordResetService.getPendingRequests(pageable);
		return ResponseEntity.ok(requestPage);
	}

	/**
	 * POST /api/password-reset/approve
	 * パスワードリセットリクエストの承認
	 * 
	 * <p>認証: ADMINロールのみ</p>
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/approve")
	public ResponseEntity<Void> approveRequest(@Valid @RequestBody PasswordResetApprovalDto dto) {
		log.info("パスワードリセットリクエスト承認: requestId={}", dto.getRequestId());
		passwordResetService.approveRequest(dto);
		return ResponseEntity.ok().build();
	}

	/**
	 * POST /api/password-reset/reject
	 * パスワードリセットリクエストの拒否
	 * 
	 * <p>認証: ADMINロールのみ</p>
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/reject")
	public ResponseEntity<Void> rejectRequest(@Valid @RequestBody PasswordResetRejectionDto dto) {
		log.info("パスワードリセットリクエスト拒否: requestId={}", dto.getRequestId());
		passwordResetService.rejectRequest(dto);
		return ResponseEntity.ok().build();
	}

	/**
	 * DELETE /api/password-reset/{requestId}
	 * パスワードリセットリクエストの削除
	 * 
	 * <p>認証: ADMINロールのみ</p>
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{requestId}")
	public ResponseEntity<Void> deleteRequest(@PathVariable UUID requestId) {
		log.info("パスワードリセットリクエスト削除: requestId={}", requestId);
		passwordResetService.deleteRequest(requestId);
		return ResponseEntity.ok().build();
	}
}
