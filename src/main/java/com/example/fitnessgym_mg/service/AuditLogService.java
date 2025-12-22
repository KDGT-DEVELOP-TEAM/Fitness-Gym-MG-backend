package com.example.fitnessgym_mg.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.response.AuditLogResponse;
import com.example.fitnessgym_mg.entity.AuditLog;
import com.example.fitnessgym_mg.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

/**
 * 監査ログサービス
 * 
 * ユースケース: UCMG-04 監査ログ
 * 全てのCRUDログを取得し、時系列で返す
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;

	/**
	 * 監査ログ一覧を取得（ページネーション対応）
	 * 作成日時の降順（最新順）で時系列に並べる
	 * 
	 * 取得されるログには以下が含まれる:
	 * - 新規レッスン作成ログ
	 * - レッスン履歴の編集ログ
	 * - レッスン履歴の削除ログ
	 * - その他のCRUD操作ログ
	 */
	public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
		Page<AuditLog> auditLogPage = auditLogRepository.findAllByCreatedAtDesc(pageable);

		return auditLogPage.map(AuditLogResponse::fromEntity);
	}
}
