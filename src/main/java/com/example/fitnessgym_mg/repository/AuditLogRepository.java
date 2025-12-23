package com.example.fitnessgym_mg.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.AuditLog;

/**
 * 監査ログリポジトリ
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

	/**
	 * 全監査ログを取得（作成日時の降順、時系列）
	 * EntityGraphでユーザー情報も取得してN+1問題を回避
	 */
	@EntityGraph(attributePaths = { "user" })
	Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
