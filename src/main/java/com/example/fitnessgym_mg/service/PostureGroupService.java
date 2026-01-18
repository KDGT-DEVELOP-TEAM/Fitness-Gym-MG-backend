package com.example.fitnessgym_mg.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.response.PostureGroupResponse;
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.Lesson;
import com.example.fitnessgym_mg.entity.PostureGroup;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.exception.ConflictException;
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.repository.PostureGroupRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 姿勢画像グループのビジネスロジック
 * DB posture_groupsテーブルの取得処理を提供
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostureGroupService {

	private final PostureGroupRepository postureGroupRepository;
	private final LessonRepository lessonRepository;
	private final AuthorizationFacade authorizationFacade;
	private final StorageService storageService;

	/**
	 * 顧客IDで姿勢画像グループ一覧をDBから取得
	 * 
	 * @deprecated 内部使用専用。Controllerからは{@link #findByCustomerIdWithAuth(User, UUID)}を使用してください。
	 * このメソッドは物理的に使用不可です。
	 * 
	 * @param customerId 顧客ID
	 * @throws UnsupportedOperationException 常にスローされる（内部専用メソッドのため）
	 */
	@Deprecated
	@Transactional(readOnly = true)
	public List<PostureGroup> findByCustomerId(UUID customerId) {
		throw new UnsupportedOperationException("内部専用メソッドです。ControllerからはfindByCustomerIdWithAuthを使用してください。");
	}

	/**
	 * 顧客IDで姿勢画像グループ一覧をDTO形式で取得（認可チェック込み）
	 * 
	 * <p>Controller層から呼び出されるメソッド。
	 * 認可チェックとDTO変換をService層で実施し、ControllerはHTTPレスポンスの生成のみに集中する。</p>
	 * 
	 * <p>各画像の署名付きURLを生成して設定します。
	 * Storageにファイルが存在しない場合は空文字列を設定し、処理を継続します。</p>
	 * 
	 * @param currentUser 現在のユーザー（認可チェック用）
	 * @param customerId 顧客ID
	 * @return 姿勢画像グループ一覧（DTO、各画像にsignedUrlが設定されている）
	 * @throws AccessDeniedException アクセス権限がない場合（HTTP 403 Forbidden）
	 */
	@Transactional(readOnly = true)
	public List<PostureGroupResponse> findByCustomerIdWithAuth(User currentUser, UUID customerId) {
		// 認可チェック: Service層で実施（Controllerは認可の詳細を知らない）
		authorizationFacade.checkCanAccessCustomerOrThrow(currentUser, customerId);

		// エンティティ取得とDTO変換
		List<PostureGroupResponse> responses = postureGroupRepository.findAllWithImagesByCustomerId(customerId)
				.stream()
				.map(PostureGroupResponse::fromEntity)
				.collect(Collectors.toList());

		// 各画像のsignedUrlを生成（並列処理でパフォーマンス最適化）
		// LessonServiceと同じパターンで実装（一貫性を保つ）
		int expiresIn = com.example.fitnessgym_mg.config.ApplicationConstants.DEFAULT_SIGNED_URL_EXPIRES_IN;
		
		// すべてのグループの画像をフラット化して並列処理
		responses.forEach(group -> {
			if (group.getImages() != null && !group.getImages().isEmpty()) {
				// LessonServiceと同じパターン: parallel().map()を使用
				List<com.example.fitnessgym_mg.dto.response.PostureImageResponse> imagesWithSignedUrls = 
					group.getImages().stream()
						.parallel() // 並列ストリームに変換して署名付きURL生成を並列化
						.map(imageResponse -> {
							try {
								// storageKeyがnullの場合は空文字列を設定
								if (imageResponse.getStorageKey() == null) {
									log.warn("Storage key is null for image: imageId={}", imageResponse.getId());
									imageResponse.setSignedUrl("");
									return imageResponse;
								}

								String originalStorageKey = imageResponse.getStorageKey();
								String signedUrl = storageService.generateSignedUrl(
										originalStorageKey, 
										expiresIn
								);
								// nullチェック: generateSignedUrlがnullを返す場合（ファイルが存在しない）
								String finalSignedUrl = signedUrl != null ? signedUrl : "";
								imageResponse.setSignedUrl(finalSignedUrl);
								
								// 検証ログ: signedUrlの生成結果を詳細に記録
								if (finalSignedUrl.isEmpty()) {
									log.warn("Generated empty signedUrl for image: imageId={}, storageKey={}, expiresIn={}", 
										imageResponse.getId(),
										originalStorageKey.length() > 100 
											? originalStorageKey.substring(0, 100) + "..." 
											: originalStorageKey,
										expiresIn);
								} else {
									log.debug("Generated signedUrl for image: imageId={}, storageKey={}, signedUrlLength={}, signedUrlPrefix={}", 
										imageResponse.getId(),
										originalStorageKey.length() > 50 
											? originalStorageKey.substring(0, 50) + "..." 
											: originalStorageKey,
										finalSignedUrl.length(),
										finalSignedUrl.length() > 100 
											? finalSignedUrl.substring(0, 100) + "..." 
											: finalSignedUrl);
								}
							} catch (Exception e) {
								log.warn("Failed to generate signed URL for image: imageId={}, storageKey={}", 
										imageResponse.getId(), 
										imageResponse.getStorageKey() != null 
											? (imageResponse.getStorageKey().length() > 100 
												? imageResponse.getStorageKey().substring(0, 100) + "..." 
												: imageResponse.getStorageKey())
											: "null",
										e);
								// エラーが発生しても続行（空文字列を設定）
								imageResponse.setSignedUrl("");
							}
							return imageResponse;
						})
						.collect(Collectors.toList());
				
				// 並列処理で生成されたsignedUrlを設定
				group.setImages(imagesWithSignedUrls);
			}
		});

		return responses;
	}

	/**
	 * IDで姿勢画像グループをDBから取得
	 * 
	 * @param postureGroupId 姿勢画像グループID
	 * @return 姿勢画像グループエンティティ
	 * @throws EntityNotFoundException DBに存在しない場合
	 */
	@Transactional(readOnly = true)
	public PostureGroup findById(UUID postureGroupId) {
		return postureGroupRepository.findById(postureGroupId)
				.orElseThrow(() -> new com.example.fitnessgym_mg.exception.EntityNotFoundException(
						"姿勢画像グループが見つかりません: " + postureGroupId));
	}

	/**
	 * 新しいレッスン記録に伴う、新しい姿勢画像群に対する空グループの作成
	 * 
	 * @deprecated 内部使用専用。Controllerからは{@link #createPostureGroupWithAuth(User, UUID)}を使用してください。
	 * このメソッドは物理的に使用不可です。
	 * 
	 * @param lessonId レッスンID
	 * @throws UnsupportedOperationException 常にスローされる（内部専用メソッドのため）
	 */
	@Deprecated
	@Transactional
	public PostureGroup createPostureGroup(UUID lessonId) {
		throw new UnsupportedOperationException("内部専用メソッドです。ControllerからはcreatePostureGroupWithAuthを使用してください。");
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
		authorizationFacade.checkCanAccessCustomerOrThrow(currentUser, customerId);

		// 冪等性チェック: 既存のposture_groupが存在する場合は409 Conflictを返す
		// 注意: このチェックはUX向上のための事前ガード。整合性はDB制約が最終責任者。
		if (postureGroupRepository.existsByLessonId(lessonId)) {
			throw new com.example.fitnessgym_mg.exception.ConflictException("このレッスンには既に姿勢画像グループが存在します");
		}

		Customer customer = lesson.getCustomer();

		// PostureGroupエンティティの作成
		PostureGroup postureGroup = new PostureGroup();
		postureGroup.setCustomer(customer);
		postureGroup.setLesson(lesson);
		postureGroup.setCapturedAt(OffsetDateTime.now(ZoneOffset.UTC));
		postureGroup.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

		try {
			PostureGroup savedPostureGroup = postureGroupRepository.save(postureGroup);

			// DTO変換して返却
			return PostureGroupResponse.fromEntity(savedPostureGroup);
		} catch (DataIntegrityViolationException e) {
			log.warn("Data integrity violation while creating posture group", e);
			// DB制約違反（UNIQUE制約）をキャッチしてConflictExceptionに変換
			// 同時リクエストやリトライ時の二重作成を防ぐ
			throw new com.example.fitnessgym_mg.exception.ConflictException("このレッスンには既に姿勢画像グループが存在します");
		}
	}
}
