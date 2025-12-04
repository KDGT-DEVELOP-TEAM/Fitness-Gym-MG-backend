package com.example.fitnessgym_mg.controller.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fitnessgym_mg.dto.response.AuditLogResponse;
import com.example.fitnessgym_mg.service.AuditLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 監査ログREST APIコントローラー
 * Reactフロントエンドとの連携用
 * 
 * ユースケース: UCMG-04 監査ログ
 * アクター: 本部
 * 概要: アプリやサーバーの監査ログを確認できる
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AuditLogApiController {

    private final AuditLogService auditLogService;

    /**
     * GET /api/admin/logs
     * 監査ログの閲覧
     * 
     * 全てのCRUDログを取得し、時系列で表示
     * 以下のログが表示される:
     * - 新規レッスン作成ログ
     * - レッスン履歴の編集ログ
     * - レッスン履歴の削除ログ
     * その他のCRUD操作ログも含む
     */
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<AuditLogResponse> auditLogPage = auditLogService.getAuditLogs(pageable);
            return ResponseEntity.ok(auditLogPage);
        } catch (Exception e) {
            log.error("監査ログ取得でエラーが発生しました: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}

