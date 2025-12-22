package com.example.fitnessgym_mg.controller.api;

import java.util.Collections;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.example.fitnessgym_mg.service.AccountService;

import jakarta.validation.Valid;
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

    /**
     * GET /api/admin/users
     * 全ユーザーアカウントの一覧取得（検索/フィルタリング可）
     */
    @GetMapping("/admin/users")
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(required = false) 
            @jakarta.validation.constraints.Size(max = 100, message = "Keyword must be less than 100 characters") 
            String name,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "created") String sort,
            @RequestParam(defaultValue = "0") 
            @jakarta.validation.constraints.Min(value = 0, message = "Page must be 0 or greater") 
            int page,
            @RequestParam(defaultValue = "10") 
            @jakarta.validation.constraints.Min(value = 1, message = "Size must be at least 1") 
            @jakarta.validation.constraints.Max(value = 100, message = "Size must not exceed 100") 
            int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserResponse> userPage = accountService.searchUsers(name, role, sort, null, pageable);
            log.debug("ユーザー一覧取得成功: page={}, size={}, total={}", page, size, userPage.getTotalElements());
            return ResponseEntity.ok(userPage);
        } catch (Exception e) {
            log.error("ユーザー一覧取得でエラーが発生しました: page={}, size={}", page, size, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/admin/users?name={keyword}
     * ユーザー名での検索（上記のgetUsersメソッドでnameパラメータとして処理）
     */

    /**
     * GET /api/admin/users/{user_id}
     * 特定ユーザー情報の取得
     */
    @GetMapping("/admin/users/{user_id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable("user_id") UUID userId) {
        try {
            UserResponse user = accountService.findById(userId, null);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("ユーザー詳細取得でエラーが発生しました: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * POST /api/admin/users
     * ユーザーアカウントの新規作成
     */
    @PostMapping("/admin/users")
    public ResponseEntity<Void> createUser(@Valid @RequestBody UserRequest request) {
        try {
            accountService.create(request, 
                    request.getStoreIds() != null ? request.getStoreIds() : Collections.emptySet());
            log.info("ユーザー作成成功: role={}", request.getRole());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            log.warn("ユーザー作成でバリデーションエラー: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("ユーザー作成でエラーが発生しました: role={}", request.getRole(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PATCH /api/admin/users/{user_id}
     * 既存ユーザーの情報更新
     */
    @PatchMapping("/admin/users/{user_id}")
    public ResponseEntity<Void> updateUser(
            @PathVariable("user_id") UUID userId,
            @Valid @RequestBody UserRequest request) {
        try {
            accountService.update(userId, request, 
                    request.getStoreIds() != null ? request.getStoreIds() : Collections.emptySet());
            log.info("ユーザー更新成功: userId={}, role={}", userId, request.getRole());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("ユーザー更新でバリデーションエラー: userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("ユーザー更新でエラーが発生しました: userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/admin/manager/users/{user_id}
     * ユーザーアカウントの削除
     * 注意: 仕様では /api/admin/manager/users/{user_id} となっているが、/api/admin/users/{user_id} の方が適切と思われる
     */
    @DeleteMapping("/admin/users/{user_id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("user_id") UUID userId) {
        try {
            accountService.delete(userId, null);
            log.info("ユーザー削除成功: userId={}", userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("ユーザー削除でエラーが発生しました: userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 店長用エンドポイント

    /**
     * GET /api/stores/{store_id}/manager/users
     * 全ユーザーアカウントの一覧取得（検索/フィルタリング可）
     */
    @GetMapping("/stores/{store_id}/manager/users")
    public ResponseEntity<Page<UserResponse>> getManagerUsers(
            @PathVariable("store_id") UUID storeId,
            @RequestParam(required = false) 
            @jakarta.validation.constraints.Size(max = 100, message = "Keyword must be less than 100 characters") 
            String name,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "created") String sort,
            @RequestParam(defaultValue = "0") 
            @jakarta.validation.constraints.Min(value = 0, message = "Page must be 0 or greater") 
            int page,
            @RequestParam(defaultValue = "10") 
            @jakarta.validation.constraints.Min(value = 1, message = "Size must be at least 1") 
            @jakarta.validation.constraints.Max(value = 100, message = "Size must not exceed 100") 
            int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserResponse> userPage = accountService.searchUsers(name, role, sort, storeId, pageable);
            return ResponseEntity.ok(userPage);
        } catch (Exception e) {
            log.error("店長用ユーザー一覧取得でエラーが発生しました: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/stores/{store_id}/manager/users?name={keyword}
     * ユーザー名での検索（上記のgetManagerUsersメソッドでnameパラメータとして処理）
     */

    /**
     * GET /api/stores/{store_id}/manager/users/{user_id}
     * 特定ユーザー情報の取得
     */
    @GetMapping("/stores/{store_id}/manager/users/{user_id}")
    public ResponseEntity<UserResponse> getManagerUser(
            @PathVariable("store_id") UUID storeId,
            @PathVariable("user_id") UUID userId) {
        try {
            UserResponse user = accountService.findById(userId, storeId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("店長用ユーザー詳細取得でエラーが発生しました: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * POST /api/stores/{store_id}/manager/users
     * ユーザーアカウントの新規作成
     */
    @PostMapping("/stores/{store_id}/manager/users")
    public ResponseEntity<Void> createManagerUser(
            @PathVariable("store_id") UUID storeId,
            @Valid @RequestBody UserRequest request) {
        try {
            accountService.create(request, 
                    request.getStoreIds() != null ? request.getStoreIds() : Collections.emptySet());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            log.warn("店長用ユーザー作成でエラーが発生しました: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("店長用ユーザー作成でエラーが発生しました: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PATCH /api/stores/{store_id}/manager/users/{user_id}
     * 既存ユーザーの情報更新
     */
    @PatchMapping("/stores/{store_id}/manager/users/{user_id}")
    public ResponseEntity<Void> updateManagerUser(
            @PathVariable("store_id") UUID storeId,
            @PathVariable("user_id") UUID userId,
            @Valid @RequestBody UserRequest request) {
        try {
            accountService.update(userId, request, 
                    request.getStoreIds() != null ? request.getStoreIds() : Collections.emptySet());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("店長用ユーザー更新でエラーが発生しました: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("店長用ユーザー更新でエラーが発生しました: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/stores/{store_id}/manager/users/{user_id}
     * ユーザーアカウントの削除
     */
    @DeleteMapping("/stores/{store_id}/manager/users/{user_id}")
    public ResponseEntity<Void> deleteManagerUser(
            @PathVariable("store_id") UUID storeId,
            @PathVariable("user_id") UUID userId) {
        try {
            accountService.delete(userId, storeId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("店長用ユーザー削除でエラーが発生しました: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

