package com.example.fitnessgym_mg.controller.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fitnessgym_mg.config.ApplicationConstants;
import com.example.fitnessgym_mg.dto.response.AuditLogResponse;
import com.example.fitnessgym_mg.service.AuditLogService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@Validated
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
     * 
     * 認可: ADMIN ロールのみ
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "0") 
            @Min(value = 0, message = "Page must be 0 or greater") 
            int page,
            @RequestParam(defaultValue = "10") 
            @Min(value = ApplicationConstants.MIN_PAGE_SIZE, message = "Size must be at least 1") 
            @Max(value = ApplicationConstants.MAX_PAGE_SIZE, message = "Invalid page size") 
            int size) {
        
        // 監査ログは時系列（作成日時の降順）で表示
        Pageable pageable = PageRequest.of(
                page, 
                size, 
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
            Page<AuditLogResponse> auditLogPage = auditLogService.getAuditLogs(pageable);
            return ResponseEntity.ok(auditLogPage);
    }
}

