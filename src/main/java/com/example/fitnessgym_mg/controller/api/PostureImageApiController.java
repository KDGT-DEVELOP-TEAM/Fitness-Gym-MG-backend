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
import com.example.fitnessgym_mg.service.PostureImageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 姿勢画像REST APIエンドポイント
 * DB posture_imagesテーブルへの操作を提供
 */
@Slf4j
@RestController
@RequestMapping("/api/posture_images")
@RequiredArgsConstructor
public class PostureImageApiController {

    private final PostureImageService postureImageService;

    /**
     * 画像アップロード
     * POST /api/posture_images/upload
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostureImageUploadResponse> uploadImage(
        @RequestParam("file") MultipartFile file,
        @RequestParam("postureGroupId") UUID postureGroupId,
        @RequestParam("position") String position,
        @RequestParam(value = "consentPublication", defaultValue = "false") boolean consentPublication,
        @RequestParam(value = "takenAt", required = false) OffsetDateTime takenAt
    ) {
        log.debug("Upload image request: groupId={}, position={}", postureGroupId, position);
        
        PostureImageUploadRequest request = new PostureImageUploadRequest();
        request.setPostureGroupId(postureGroupId);
        request.setPosition(position);
        request.setConsentPublication(consentPublication);
        request.setTakenAt(takenAt);
        
        PostureImageUploadResponse response = postureImageService.uploadImage(file, request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 署名付きURL生成
     * GET /api/posture_images/{imageId}/signed-url
     */
    @GetMapping("/{imageId}/signed-url")
    public ResponseEntity<SignedUrlResponse> getSignedUrl(
        @PathVariable UUID imageId,
        @RequestParam(defaultValue = "3600") 
        @jakarta.validation.constraints.Min(value = 60, message = "ExpiresIn must be at least 60 seconds") 
        @jakarta.validation.constraints.Max(value = 604800, message = "ExpiresIn must not exceed 604800 seconds (7 days)") 
        int expiresIn
    ) {
        log.debug("Generate signed URL request: imageId={}, expiresIn={}", imageId, expiresIn);
        
        SignedUrlResponse response = postureImageService.generateSignedUrl(imageId, expiresIn);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * バッチ署名付きURL生成
     * POST /api/posture_images/signed-urls
     */
    @PostMapping("/signed-urls")
    public ResponseEntity<BatchSignedUrlResponse> getBatchSignedUrls(
        @RequestBody @Valid BatchSignedUrlRequest request
    ) {
        log.debug("Generate batch signed URLs request: imageIds count={}", request.getImageIds().size());
        
        BatchSignedUrlResponse response = postureImageService.generateBatchSignedUrls(
            request.getImageIds(), 
            request.getExpiresIn()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 画像削除（Storage + DB）
     * DELETE /api/posture_images/{imageId}
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID imageId) {
        log.debug("Delete image request: imageId={}", imageId);
        
        postureImageService.deleteImageWithStorage(imageId);
        
        return ResponseEntity.noContent().build();
    }
}
