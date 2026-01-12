package com.example.fitnessgym_mg.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.response.AuditLogResponse;
import com.example.fitnessgym_mg.entity.AuditLog;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.ActionType;
import com.example.fitnessgym_mg.entity.enums.TargetTableType;
import com.example.fitnessgym_mg.repository.AuditLogRepository;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 監査ログサービス
 * 
 * ユースケース: UCMG-04 監査ログ
 * 全てのCRUDログを取得し、時系列で返す
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;
	private final SecurityUtil securityUtil;
	private final AccountAuthorizationService accountAuthorizationService;

	/**
	 * 監査ログを記録する
	 * 
	 * <p>監査ログ記録に失敗しても、呼び出し元の処理は継続する（監査ログは補助的な機能）</p>
	 * 
	 * @param action 操作種別（CREATE, UPDATE, DELETE）
	 * @param targetTable 対象テーブル
	 * @param targetId 対象レコードID（UUID）
	 * @param user 操作を実行したユーザー
	 */
	@Transactional
	public void recordAuditLog(ActionType action, TargetTableType targetTable, UUID targetId, User user) {
		try {
			AuditLog auditLog = AuditLog.builder()
					.action(action)
					.targetTable(targetTable)
					.targetId(targetId)
					.user(user)
					.build();
			
			auditLogRepository.save(auditLog);
			log.debug("監査ログを記録しました: action={}, targetTable={}, targetId={}, userId={}", 
					action, targetTable, targetId, user.getId());
		} catch (Exception e) {
			// 監査ログ記録に失敗しても、呼び出し元の処理は継続
			log.error("監査ログの記録に失敗しました: action={}, targetTable={}, targetId={}, userId={}", 
					action, targetTable, targetId, user != null ? user.getId() : null, e);
		}
	}

	/**
	 * 監査ログ一覧を取得（ページネーション対応）
	 * 作成日時の降順（最新順）で時系列に並べる
	 * 
	 * <p>認可: ADMINロールのみ閲覧可能。Service層で認可チェックを実施。</p>
	 * 
	 * 取得されるログには以下が含まれる:
	 * - 新規レッスン作成ログ
	 * - レッスン履歴の編集ログ
	 * - レッスン履歴の削除ログ
	 * - その他のCRUD操作ログ
	 * 
	 * @param pageable ページネーション情報
	 * @return 監査ログのページ
	 * @throws com.example.fitnessgym_mg.exception.AccessDeniedException ADMINロールでない場合
	 */
	@Transactional(readOnly = true)
	public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
		// 認可チェック: ADMINロールのみ閲覧可能
		User currentUser = securityUtil.getCurrentUserOrThrow();
		accountAuthorizationService.checkCanViewAuditLogs(currentUser);
		
		Page<AuditLog> auditLogPage = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);

		return auditLogPage.map(AuditLogResponse::fromEntity);
	}
}
