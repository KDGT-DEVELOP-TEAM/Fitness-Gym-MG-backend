package com.example.fitnessgym_mg.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.fitnessgym_mg.dto.request.PostureImageUploadRequest;
import com.example.fitnessgym_mg.dto.response.BatchSignedUrlResponse;
import com.example.fitnessgym_mg.dto.response.PostureImageUploadResponse;
import com.example.fitnessgym_mg.dto.response.SignedUrlResponse;
import com.example.fitnessgym_mg.entity.PostureGroup;
import com.example.fitnessgym_mg.entity.PostureImage;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;
import com.example.fitnessgym_mg.repository.PostureImageRepository;
import com.example.fitnessgym_mg.repository.PostureGroupRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 姿勢画像のビジネスロジック
 * DB posture_imagesテーブルへの操作を提供
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostureImageService {

    private final PostureImageRepository postureImageRepository;
    private final PostureGroupRepository postureGroupRepository;
    private final SupabaseStorageService storageService;
    private final CustomerAuthorizationService customerAuthorizationService;
    
    // 許可するContent-Type
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg", 
        "image/jpg", 
        "image/png", 
        "image/webp"
    );
    
    // 許可する拡張子
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    
    private static final long MAX_FILE_SIZE = com.example.fitnessgym_mg.config.ApplicationConstants.MAX_FILE_SIZE_BYTES;

    /**
     * グループIDで姿勢画像リストをDBから取得（撮影日時昇順）
     */
    @Transactional(readOnly = true)
    public List<PostureImage> findByGroupId(UUID postureGroupId) {
        return postureImageRepository.findByPostureGroupIdOrderByTakenAtAsc(postureGroupId);
    }

    /**
     * 姿勢画像をDBから削除
     * 注意: Storageの画像ファイル自体は削除されない
     * @Transactional: DBへの書き込みトランザクション
     */
    @Transactional
    public void delete(UUID postureImageId) {
        if (!postureImageRepository.existsById(postureImageId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Posture image not found: " + postureImageId);
        }
        postureImageRepository.deleteById(postureImageId);
    }

    /**
     * 画像をアップロード
     * @param file アップロードするファイル
     * @param request アップロードリクエスト
     * @return アップロード結果
     * @deprecated 内部使用専用。Controllerからは{@link #uploadImageWithAuth(User, MultipartFile, PostureImageUploadRequest)}を使用してください。
     */
    @Deprecated
    @Transactional
    public PostureImageUploadResponse uploadImage(MultipartFile file, PostureImageUploadRequest request) {
        // 1. ファイルバリデーション
        validateImageFile(file);
        
        // 2. PostureGroupからcustomerIdを取得
        PostureGroup group = postureGroupRepository.findById(request.getPostureGroupId())
            .orElseThrow(() -> new IllegalArgumentException("PostureGroup not found: " + request.getPostureGroupId()));
        UUID customerId = group.getCustomer().getId();
        
        // 3. positionのEnum変換
        PostureImagePosition position = PostureImagePosition.fromCode(request.getPosition());
        
        // 4. storageKey生成
        String storageKey = generateStorageKey(customerId, request.getPostureGroupId(), position);
        
        // 5. 既存画像を検索・削除
        Optional<PostureImage> existingImage = postureImageRepository
            .findByPostureGroupIdAndPosition(request.getPostureGroupId(), position);
        
        if (existingImage.isPresent()) {
            log.info("Deleting existing image: position={}, storageKey={}", 
                position, existingImage.get().getStorageKey());
            deleteImageWithStorage(existingImage.get().getId());
        }
        
        // 6. Storageにアップロード
        String uploadedStorageKey;
        try {
            uploadedStorageKey = storageService.uploadFile(file, storageKey);
        } catch (Exception e) {
            log.error("Failed to upload file to Storage", e);
            throw new RuntimeException("Storage upload failed", e);
        }
        
        // 7. DBに保存
        PostureImage newImage = PostureImage.builder()
                .postureGroup(group)
                .storageKey(uploadedStorageKey)
                .position(position)
                .consentPublication(request.isConsentPublication())
                .takenAt(request.getTakenAt() != null ? request.getTakenAt() : OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();
        
        try {
            postureImageRepository.save(newImage);
        } catch (Exception e) {
            // ロールバック: Storageから削除
            log.error("Failed to save image metadata to DB, rolling back Storage upload", e);
            storageService.deleteFile(uploadedStorageKey);
            throw new RuntimeException("Failed to save image metadata", e);
        }
        
        // 8. 署名付きURL生成
        String signedUrl = storageService.generateSignedUrl(uploadedStorageKey, 
            com.example.fitnessgym_mg.config.ApplicationConstants.DEFAULT_SIGNED_URL_EXPIRES_IN);
        
        // 9. レスポンス作成
        return PostureImageUploadResponse.builder()
            .id(newImage.getId())
            .postureGroupId(newImage.getPostureGroup().getId())
            .storageKey(newImage.getStorageKey())
            .position(newImage.getPosition().getCode())
            .takenAt(newImage.getTakenAt())
            .createdAt(newImage.getCreatedAt())
            .signedUrl(signedUrl)
            .consentPublication(newImage.isConsentPublication())
            .build();
    }
    
    /**
     * 画像をアップロード（認可チェック込み）
     * 
     * <p>Controller層から呼び出されるメソッド。
     * 認可チェックとDTO変換をService層で実施し、ControllerはHTTPレスポンスの生成のみに集中する。</p>
     * 
     * @param currentUser 現在のユーザー（認可チェック用）
     * @param file アップロードするファイル
     * @param request アップロードリクエスト
     * @return アップロード結果
     * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
     */
    @Transactional
    public PostureImageUploadResponse uploadImageWithAuth(
        User currentUser, 
        MultipartFile file, 
        PostureImageUploadRequest request
    ) {
        // 認可チェック: PostureGroupへのアクセス権限を確認
        PostureGroup group = postureGroupRepository.findById(request.getPostureGroupId())
            .orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("PostureGroup not found: " + request.getPostureGroupId()));
        
        UUID customerId = group.getCustomer().getId();
        customerAuthorizationService.checkCanAccessCustomerOrThrow(currentUser, customerId);
        
        // 既存のuploadImageロジックを実行
        return uploadImage(file, request);
    }
    
    /**
     * 署名付きURLを生成
     * @param imageId 画像ID
     * @param expiresIn 有効期限（秒）
     * @return 署名付きURL
     * @deprecated 内部使用専用。Controllerからは{@link #generateSignedUrlWithAuth(User, UUID, int)}を使用してください。
     */
    @Deprecated
    public SignedUrlResponse generateSignedUrl(UUID imageId, int expiresIn) {
        PostureImage image = postureImageRepository.findById(imageId)
            .orElseThrow(() -> new IllegalArgumentException("PostureImage not found: " + imageId));
        
        String signedUrl = storageService.generateSignedUrl(image.getStorageKey(), expiresIn);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(expiresIn);
        
        return new SignedUrlResponse(signedUrl, expiresAt);
    }
    
    /**
     * 署名付きURLを生成（認可チェック込み）
     * 
     * @param currentUser 現在のユーザー（認可チェック用）
     * @param imageId 画像ID
     * @param expiresIn 有効期限（秒）
     * @return 署名付きURL
     * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
     */
    public SignedUrlResponse generateSignedUrlWithAuth(User currentUser, UUID imageId, int expiresIn) {
        // 認可チェック: 画像へのアクセス権限を確認
        customerAuthorizationService.checkCanAccessPostureImageOrThrow(currentUser, imageId);
        
        // 既存のgenerateSignedUrlロジックを実行
        return generateSignedUrl(imageId, expiresIn);
    }
    
    /**
     * バッチで署名付きURLを生成
     * @param imageIds 画像IDリスト
     * @param expiresIn 有効期限（秒）
     * @return バッチ署名付きURLレスポンス
     * @deprecated 内部使用専用。Controllerからは{@link #generateBatchSignedUrlsWithAuth(User, List, int)}を使用してください。
     */
    @Deprecated
    public BatchSignedUrlResponse generateBatchSignedUrls(List<UUID> imageIds, int expiresIn) {
        List<PostureImage> images = postureImageRepository.findAllByIdIn(imageIds);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(expiresIn);
        
        List<BatchSignedUrlResponse.ImageSignedUrl> urls = images.parallelStream()
            .map(img -> {
                try {
                    String signedUrl = storageService.generateSignedUrl(img.getStorageKey(), expiresIn);
                    return new BatchSignedUrlResponse.ImageSignedUrl(img.getId(), signedUrl, expiresAt);
                } catch (Exception e) {
                    log.error("Failed to generate signed URL for image: {}", img.getId(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        return new BatchSignedUrlResponse(urls);
    }
    
    /**
     * バッチ署名付きURLを生成（認可チェック込み）
     * 
     * @param currentUser 現在のユーザー（認可チェック用）
     * @param imageIds 画像IDリスト
     * @param expiresIn 有効期限（秒）
     * @return バッチ署名付きURL
     * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
     */
    public BatchSignedUrlResponse generateBatchSignedUrlsWithAuth(
        User currentUser, 
        List<UUID> imageIds, 
        int expiresIn
    ) {
        // 各画像へのアクセス権限を確認
        for (UUID imageId : imageIds) {
            customerAuthorizationService.checkCanAccessPostureImageOrThrow(currentUser, imageId);
        }
        
        // 既存のgenerateBatchSignedUrlsロジックを実行
        return generateBatchSignedUrls(imageIds, expiresIn);
    }
    
    /**
     * 画像をStorageとDBから削除
     * @param imageId 画像ID
     * @deprecated 内部使用専用。Controllerからは{@link #deleteImageWithStorageWithAuth(User, UUID)}を使用してください。
     */
    @Deprecated
    @Transactional
    public void deleteImageWithStorage(UUID imageId) {
        PostureImage image = postureImageRepository.findById(imageId)
            .orElseThrow(() -> new IllegalArgumentException("PostureImage not found: " + imageId));
        
        // Storageから削除
        storageService.deleteFile(image.getStorageKey());
        
        // DBから削除
        postureImageRepository.deleteById(imageId);
        
        log.info("Successfully deleted image with storage: imageId={}, storageKey={}", 
            imageId, image.getStorageKey());
    }
    
    /**
     * 画像をStorageとDBから削除（認可チェック込み）
     * 
     * @param currentUser 現在のユーザー（認可チェック用）
     * @param imageId 画像ID
     * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
     */
    @Transactional
    public void deleteImageWithStorageWithAuth(User currentUser, UUID imageId) {
        // 認可チェック: 画像へのアクセス権限を確認
        customerAuthorizationService.checkCanAccessPostureImageOrThrow(currentUser, imageId);
        
        // 既存のdeleteImageWithStorageロジックを実行
        deleteImageWithStorage(imageId);
    }
    
    // ===== ヘルパーメソッド =====
    
    /**
     * ファイルバリデーション
     * ファイルサイズ、Content-Type、拡張子をチェック
     */
    private void validateImageFile(MultipartFile file) {
        // 空ファイルチェック
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        
        // ファイルサイズチェック
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                String.format("File size exceeds limit: %dMB", 
                    com.example.fitnessgym_mg.config.ApplicationConstants.MAX_FILE_SIZE_MB)
            );
        }
        
        // Content-Typeチェック
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are allowed");
        }
        
        // 拡張子チェック
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = getFileExtension(originalFilename).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException("File extension must be jpg, jpeg, png, or webp");
            }
        }
    }
    
    /**
     * ファイル名から拡張子を取得
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
    
    /**
     * storageKey生成
     * パターン: postures/{customerId}/{groupId}/{position}.jpg
     */
    private String generateStorageKey(UUID customerId, UUID groupId, PostureImagePosition position) {
        return String.format("postures/%s/%s/%s.jpg", 
            customerId, groupId, position.getCode());
    }
}
