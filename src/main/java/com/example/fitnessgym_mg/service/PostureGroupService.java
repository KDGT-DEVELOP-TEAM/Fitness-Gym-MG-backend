package com.example.fitnessgym_mg.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.fitnessgym_mg.dto.response.PostureGroupResponse;
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.Lesson;
import com.example.fitnessgym_mg.entity.PostureGroup;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.repository.PostureGroupRepository;
import com.example.fitnessgym_mg.service.CustomerAuthorizationService;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

/**
 * 姿勢画像グループのビジネスロジック
 * DB posture_groupsテーブルの取得処理を提供
 */
@Service
@RequiredArgsConstructor
public class PostureGroupService {

    private final PostureGroupRepository postureGroupRepository;
    private final LessonRepository lessonRepository;
    private final SecurityUtil securityUtil;
    private final CustomerAuthorizationService customerAuthorizationService;

    /**
     * 顧客IDで姿勢画像グループ一覧をDBから取得
     * @Transactional(readOnly = true): DBへの読み取り専用トランザクション
     * 
     * @deprecated 内部使用専用。Controllerからは{@link #findByCustomerIdWithAuth(User, UUID)}を使用してください。
     */
    @Deprecated
    @Transactional(readOnly = true)
    public List<PostureGroup> findByCustomerId(UUID customerId) {
        // 認可チェック: Service層での最終防衛ライン（Controller層での早期リターンとは別）
        User currentUser = securityUtil.getCurrentUserOrThrow();
        customerAuthorizationService.checkCanAccessCustomerOrThrow(currentUser, customerId);
        
        return postureGroupRepository.findAllWithImagesByCustomerId(customerId);
    }
    
    /**
     * 顧客IDで姿勢画像グループ一覧をDTO形式で取得（認可チェック込み）
     * 
     * <p>Controller層から呼び出されるメソッド。
     * 認可チェックとDTO変換をService層で実施し、ControllerはHTTPレスポンスの生成のみに集中する。</p>
     * 
     * @param currentUser 現在のユーザー（認可チェック用）
     * @param customerId 顧客ID
     * @return 姿勢画像グループ一覧（DTO）
     * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
     */
    @Transactional(readOnly = true)
    public List<PostureGroupResponse> findByCustomerIdWithAuth(User currentUser, UUID customerId) {
        // 認可チェック: Service層で実施（Controllerは認可の詳細を知らない）
        customerAuthorizationService.checkCanAccessCustomerOrThrow(currentUser, customerId);
        
        // エンティティ取得とDTO変換
        return postureGroupRepository.findAllWithImagesByCustomerId(customerId)
                .stream()
                .map(PostureGroupResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * IDで姿勢画像グループをDBから取得
     * @throws ResponseStatusException DBに存在しない場合404
     */
    @Transactional(readOnly = true)
    public PostureGroup findById(UUID postureGroupId) {
        return postureGroupRepository.findById(postureGroupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Posture group not found: " + postureGroupId));
    }

    /**
     * 新しいレッスン記録に伴う、新しい姿勢画像群に対する空グループの作成
     * 
     * @deprecated 内部使用専用。Controllerからは{@link #createPostureGroupWithAuth(User, UUID)}を使用してください。
     */
    @Deprecated
    @Transactional
    public PostureGroup createPostureGroup(UUID lessonId) {
        // レッスンを取得
        Lesson lesson = lessonRepository.findByIdWithRelations(lessonId)
                .orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("レッスンが見つかりません"));
        
        // レッスンに紐づく顧客IDを取得
        if (lesson.getCustomer() == null) {
            throw new com.example.fitnessgym_mg.exception.EntityNotFoundException("レッスンに顧客情報が紐づいていません");
        }
        UUID customerId = lesson.getCustomer().getId();
        
        // 認可チェック: Service層での最終防衛ライン（Controller層での早期リターンとは別）
        User currentUser = securityUtil.getCurrentUserOrThrow();
        customerAuthorizationService.checkCanAccessCustomerOrThrow(currentUser, customerId);
        
        // 冪等性チェック: 既存のposture_groupが存在する場合は409 Conflictを返す
        if (postureGroupRepository.existsByLessonId(lessonId)) {
            throw new com.example.fitnessgym_mg.exception.ConflictException("このレッスンには既に姿勢画像グループが存在します");
        }
        
        Customer customer = lesson.getCustomer();
        
        // PostureGroupエンティティの作成
        PostureGroup postureGroup = new PostureGroup();
        postureGroup.setCustomer(customer);
        postureGroup.setLesson(lesson);
        postureGroup.setCapturedAt(OffsetDateTime.now());
        postureGroup.setCreatedAt(OffsetDateTime.now());
        
        return postureGroupRepository.save(postureGroup);
    }
    
    /**
     * レッスンIDで姿勢画像グループを作成（認可チェック込み）
     * 
     * <p>Controller層から呼び出されるメソッド。
     * 認可チェックをService層で実施し、ControllerはHTTPレスポンスの生成のみに集中する。</p>
     * 
     * @param currentUser 現在のユーザー（認可チェック用）
     * @param lessonId レッスンID
     * @return 作成された姿勢画像グループ（DTO）
     * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
     * @throws ConflictException 既に姿勢画像グループが存在する場合
     */
    @Transactional
    public PostureGroupResponse createPostureGroupWithAuth(User currentUser, UUID lessonId) {
        // レッスンを取得
        Lesson lesson = lessonRepository.findByIdWithRelations(lessonId)
                .orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException("レッスンが見つかりません"));
        
        // レッスンに紐づく顧客IDを取得
        if (lesson.getCustomer() == null) {
            throw new com.example.fitnessgym_mg.exception.EntityNotFoundException("レッスンに顧客情報が紐づいていません");
        }
        UUID customerId = lesson.getCustomer().getId();
        
        // 認可チェック: Service層で実施（Controllerは認可の詳細を知らない）
        customerAuthorizationService.checkCanAccessCustomerOrThrow(currentUser, customerId);
        
        // 冪等性チェック: 既存のposture_groupが存在する場合は409 Conflictを返す
        if (postureGroupRepository.existsByLessonId(lessonId)) {
            throw new com.example.fitnessgym_mg.exception.ConflictException("このレッスンには既に姿勢画像グループが存在します");
        }
        
        Customer customer = lesson.getCustomer();
        
        // PostureGroupエンティティの作成
        PostureGroup postureGroup = new PostureGroup();
        postureGroup.setCustomer(customer);
        postureGroup.setLesson(lesson);
        postureGroup.setCapturedAt(OffsetDateTime.now());
        postureGroup.setCreatedAt(OffsetDateTime.now());
        
        PostureGroup savedPostureGroup = postureGroupRepository.save(postureGroup);
        
        // DTO変換して返却
        return PostureGroupResponse.fromEntity(savedPostureGroup);
    }
}

