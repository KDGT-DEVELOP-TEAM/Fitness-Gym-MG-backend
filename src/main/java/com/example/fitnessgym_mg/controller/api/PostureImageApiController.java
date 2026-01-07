package com.example.fitnessgym_mg.controller.api;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.fitnessgym_mg.dto.request.BatchSignedUrlRequest;
import com.example.fitnessgym_mg.dto.request.PostureImageUploadRequest;
import com.example.fitnessgym_mg.dto.response.BatchSignedUrlResponse;
import com.example.fitnessgym_mg.dto.response.PostureImageUploadResponse;
import com.example.fitnessgym_mg.dto.response.SignedUrlResponse;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;
import com.example.fitnessgym_mg.exception.InvalidRequestException;
import com.example.fitnessgym_mg.service.PostureImageService;
import com.example.fitnessgym_mg.util.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 姿勢画像REST APIエンドポイント
 * DB posture_imagesテーブルへの操作を提供
 */
/**
 * 姿勢画像REST APIエンドポイント
 * Controllerの責務: HTTPリクエスト/レスポンスの制御のみ。
 * 認可チェックとファイルバリデーションはService層で実施される。
 */
@Slf4j
@RestController
@RequestMapping("/api/posture_images")
@RequiredArgsConstructor
public class PostureImageApiController {

    private final PostureImageService postureImageService;
    private final SecurityUtil securityUtil;

    /**
     * 画像アップロード
     * POST /api/posture_images/upload
     * 
     * <p>Controllerの責務: HTTPリクエスト/レスポンスの制御のみ。
     * 認可チェックとファイルバリデーションはService層で実施される。</p>
     * 
     * <p>positionパラメータ: フロントエンドから小文字コード（front/right/back/left）を受け取り、
     * PostureImagePosition.fromCode()でEnumに変換します。不正な値の場合は400エラーを返します。</p>
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostureImageUploadResponse> uploadImage(
        @RequestParam MultipartFile file,
        @RequestParam UUID postureGroupId,
        @RequestParam String position,
        @RequestParam(defaultValue = "false") boolean consentPublication,
        @RequestParam(required = false) OffsetDateTime takenAt
    ) {
        log.debug("Upload image request: groupId={}, position={}", postureGroupId, position);
        
        // positionをStringからEnumに変換（不正値は400エラー）
        PostureImagePosition positionEnum;
        try {
            positionEnum = PostureImagePosition.fromCode(position);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid position value: " + position + ". Must be one of: front, right, back, left");
        }
        
        // 現在のユーザーを取得
        User currentUser = securityUtil.getCurrentUserOrThrow();
        
        PostureImageUploadRequest request = new PostureImageUploadRequest();
        request.setPostureGroupId(postureGroupId);
        request.setPosition(positionEnum);
        request.setConsentPublication(consentPublication);
        request.setTakenAt(takenAt);
        
        // Service層で認可チェック、ファイルバリデーション、アップロードを実施
        PostureImageUploadResponse response = postureImageService.uploadImageWithAuth(
            currentUser, file, request
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 署名付きURL生成
     * GET /api/posture_images/{imageId}/signed-url
     * 
     * <p>Controllerの責務: HTTPリクエスト/レスポンスの制御のみ。
     * 認可チェックはService層で実施される。</p>
     */
    @GetMapping("/{imageId}/signed-url")
    public ResponseEntity<SignedUrlResponse> getSignedUrl(
        @PathVariable UUID imageId,
        @RequestParam(defaultValue = "" + com.example.fitnessgym_mg.config.ApplicationConstants.DEFAULT_SIGNED_URL_EXPIRES_IN) 
        @jakarta.validation.constraints.Min(value = com.example.fitnessgym_mg.config.ApplicationConstants.MIN_SIGNED_URL_EXPIRES_IN, message = "ExpiresIn must be at least 60 seconds") 
        @jakarta.validation.constraints.Max(value = com.example.fitnessgym_mg.config.ApplicationConstants.MAX_SIGNED_URL_EXPIRES_IN, message = "ExpiresIn must not exceed 604800 seconds (7 days)") 
        int expiresIn
    ) {
        log.debug("Generate signed URL request: imageId={}, expiresIn={}", imageId, expiresIn);
        
        // 現在のユーザーを取得
        User currentUser = securityUtil.getCurrentUserOrThrow();
        
        // Service層で認可チェックと署名付きURL生成を実施
        SignedUrlResponse response = postureImageService.generateSignedUrlWithAuth(
            currentUser, imageId, expiresIn
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * バッチ署名付きURL生成
     * POST /api/posture_images/signed-urls
     * 
     * <p>Controllerの責務: HTTPリクエスト/レスポンスの制御のみ。
     * 認可チェックはService層で実施される。</p>
     */
    @PostMapping("/signed-urls")
    public ResponseEntity<BatchSignedUrlResponse> getBatchSignedUrls(
        @RequestBody @Valid BatchSignedUrlRequest request
    ) {
        log.debug("Generate batch signed URLs request: imageIds count={}", request.getImageIds().size());
        
        // 現在のユーザーを取得
        User currentUser = securityUtil.getCurrentUserOrThrow();
        
        // Service層で認可チェックとバッチ署名付きURL生成を実施
        BatchSignedUrlResponse response = postureImageService.generateBatchSignedUrlsWithAuth(
            currentUser,
            request.getImageIds(), 
            request.getExpiresIn()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 画像削除（Storage + DB）
     * DELETE /api/posture_images/{imageId}
     * 
     * <p>Controllerの責務: HTTPリクエスト/レスポンスの制御のみ。
     * 認可チェックはService層で実施される。</p>
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID imageId) {
        log.debug("Delete image request: imageId={}", imageId);
        
        // 現在のユーザーを取得
        User currentUser = securityUtil.getCurrentUserOrThrow();
        
        // Service層で認可チェックと削除を実施
        postureImageService.deleteImageWithStorageWithAuth(currentUser, imageId);
        
        return ResponseEntity.noContent().build();
    }
}
