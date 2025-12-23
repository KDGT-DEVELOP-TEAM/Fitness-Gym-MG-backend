package com.example.fitnessgym_mg.service;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors; // StoreエンティティのSetに変換するために追加

import jakarta.persistence.criteria.JoinType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.request.UserRequest;
import com.example.fitnessgym_mg.dto.response.UserResponse;
import com.example.fitnessgym_mg.entity.Store;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.repository.StoreRepository;
import com.example.fitnessgym_mg.repository.UserRepository;
import com.example.fitnessgym_mg.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

	private final UserRepository userRepository;
	private final StoreRepository storeRepository;
	private final PasswordEncoder passwordEncoder;
	private final LessonRepository lessonRepository;
	private final SecurityUtil securityUtil;

	// --- ユーザー検索 ---
	@Transactional(readOnly = true)
	public Page<UserResponse> searchUsers(
			String keyword,
			String role,
			String sort,
			UUID storeId, // 検索条件のstoreIdは単一でOK
			Pageable pageable) {

		Specification<User> spec = (root, query, cb) -> null;

		// --- 1-1. 店舗IDによる絞り込み (中間テーブル user_stores 経由) ---
		if (storeId != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.join("stores", JoinType.INNER).get("id"), storeId));
		}

		// --- 1-2. キーワードによる絞り込み ---
		if (keyword != null && !keyword.isEmpty()) {
			String lowerKeyword = keyword.toLowerCase();
			spec = spec.and((root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("name")), "%" + lowerKeyword + "%"),
					cb.like(cb.lower(root.get("kana")), "%" + lowerKeyword + "%")));
		}

		// --- 1-3. ロールによる絞り込み ---
		if (role != null && !role.isEmpty()) {
			try {
				UserRole roleEnum = UserRole.valueOf(role.toUpperCase());
				spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), roleEnum));
			} catch (IllegalArgumentException ignored) {
			}
		}

		// 2. ソートオブジェクトの生成 (グルーピングソート対応)
		Sort sortObj = createSort(sort);

		Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortObj);

		// 3. 検索の実行
		Page<User> users = userRepository.findAll(spec, sortedPageable);

		return users.map(UserResponse::fromEntity);
	}

	// ユーザー作成
	public void create(UserRequest req, Set<UUID> storeIds) {

		// マネージャーの権限チェック: マネージャーはトレーナーのみ作成可能
		validateManagerPermission(req.getRole());

		// 1. 店長ロールのバリデーション (単一の店舗必須)
		validateManagerRole(req.getRole(), storeIds);
		
		if (req.getRole() == UserRole.TRAINER) {
			// トレーナーの場合、店舗は0個以上でOK。ただし、Setがnullの場合は空Setとして扱う
			if (storeIds == null) {
				storeIds = Collections.emptySet();
			}
		} else {
			// ADMINの場合、店舗は不要
			storeIds = Collections.emptySet();
		}

		// パスワードの必須チェック (UserRequestで@NotBlankを外したため、ここで補完)
		if (req.getPass() == null || req.getPass().trim().isEmpty()) {
			throw new IllegalArgumentException("パスワードは必須です。");
		}

		// 2. ユーザーの基本情報設定
		User user = new User();
		setUserBasicFields(user, req);
		user.setPassword(passwordEncoder.encode(req.getPass()));

		// 3. 店舗の紐づけ
		Set<Store> storesToAssign = validateAndGetStores(storeIds);
		user.setStores(storesToAssign);
		userRepository.save(user);
	}

	// 更新
	public void update(UUID id, UserRequest req, Set<UUID> storeIds) {
		User user = findUserById(id, null);

		// マネージャーの権限チェック: マネージャーはトレーナーのみ編集可能
		validateManagerPermission(user.getRole(), req.getRole());

		// StoreIdsのnullチェック (フロントから空配列[]が来る想定だが念のため)
		if (storeIds == null) {
			storeIds = Collections.emptySet();
		}

		// 1. 店長ロールのバリデーション (単一の店舗必須)
		validateManagerRole(req.getRole(), storeIds);

		// 既存の更新ロジック
		setUserBasicFields(user, req);

		if (req.getPass() != null && !req.getPass().isEmpty()) {
			user.setPassword(passwordEncoder.encode(req.getPass()));
		}

		// 2. 店舗の紐づけ情報の上書き
		Set<Store> storesToAssign = validateAndGetStores(storeIds);
		user.setStores(storesToAssign);
		userRepository.save(user);
	}

	// --- ユーザーを有効化 ---
	public void enableActive(UUID id, UUID storeId) {
		User user = findUserById(id, storeId);
		
		// マネージャーの権限チェック: マネージャーはトレーナーのみ編集可能
		validateManagerPermission(user.getRole());
		
		if (user.isActive()) {
			return;
		}
		user.setActive(true);
		userRepository.save(user);
	}

	// --- ユーザーを無効化 ---
	public void disableActive(UUID id, UUID storeId) {
		User user = findUserById(id, storeId);
		
		// マネージャーの権限チェック: マネージャーはトレーナーのみ編集可能
		validateManagerPermission(user.getRole());
		
		if (!user.isActive()) {
			return;
		}
		user.setActive(false);
		userRepository.save(user);
	}

	// 削除
	public void delete(UUID id, UUID storeId) {
		User user = findUserById(id, storeId);

		if (user.isActive()) {
			throw new IllegalStateException("有効ユーザーは削除できません");
		}

		if (hasRelatedData(id)) {
			throw new IllegalStateException("このユーザーにはレッスン履歴が紐づいているため、削除できません。無効化してください。");
		}

		userRepository.delete(user);
	}

	// idでアカウント情報を取得
	@Transactional(readOnly = true)
	public UserResponse findById(UUID id, UUID storeId) {
		User user = findUserById(id, storeId);
		return UserResponse.fromEntity(user);
	}

	// --- ヘルパーメソッド ---

	/**
	 * ユーザーの基本情報フィールドを設定（共通ロジック）
	 * 
	 * @param user ユーザーエンティティ
	 * @param req ユーザーリクエストDTO
	 */
	private void setUserBasicFields(User user, UserRequest req) {
		user.setEmail(req.getEmail());
		user.setName(req.getName());
		user.setKana(req.getKana());
		user.setRole(req.getRole());
		user.setActive(req.isActive());
	}

	private User findUserById(UUID id, UUID storeId) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> {
					// システムエラーではなく、ビジネスロジックエラー（エンティティが見つからない）
					// log.warnを使用（log.errorではない）
					return new com.example.fitnessgym_mg.exception.EntityNotFoundException("User not found with id: " + id);
				});

		// 店長の場合 (storeId != null)、操作対象のユーザーが自分の店舗に属するかチェック
		if (storeId != null) {
			// ユーザーが自分の店舗に属さない場合は拒否
			if (user.getStores() == null || user.getStores().isEmpty()) {
				// ビジネスロジックエラー（権限エラー）なのでlog.warnを使用
				throw new com.example.fitnessgym_mg.exception.EntityNotFoundException("User not found (or access denied) with id: " + id);
			}
			
			boolean isAssignedToStore = user.getStores().stream()
					.anyMatch(store -> store.getId().equals(storeId));

			if (!isAssignedToStore) {
				// ビジネスロジックエラー（権限エラー）なのでlog.warnを使用
				throw new com.example.fitnessgym_mg.exception.EntityNotFoundException("User not found (or access denied) with id: " + id);
			}
		}
		return user;
	}

	private Sort createSort(String sort) {
		// 1. ロールによる昇順ソート（roleOrderプロパティは存在しないため、roleでソート）
		Sort primarySort = Sort.by("role").ascending();

		// 2. 登録日時降順ソート、またはカナ昇順ソート
		Sort secondarySort;
		if ("kana".equals(sort)) {
			secondarySort = Sort.by("kana").ascending();
		} else {
			// デフォルトおよび "created" の場合、登録日時降順
			secondarySort = Sort.by("createdAt").descending();
		}

		return primarySort.and(secondarySort);
	}

	private boolean hasRelatedData(UUID userId) {
		return lessonRepository.countByTrainerId(userId) > 0;
	}

	/**
	 * 店舗IDの検証と取得（共通ロジック）
	 * 
	 * @param storeIds 店舗IDのセット
	 * @return 検証済みの店舗エンティティのセット
	 */
	private Set<Store> validateAndGetStores(Set<UUID> storeIds) {
		if (storeIds == null || storeIds.isEmpty()) {
			return Collections.emptySet();
		}
		
		Set<Store> stores = storeRepository.findAllById(storeIds).stream()
				.collect(Collectors.toSet());
		
		if (stores.size() != storeIds.size()) {
			throw new com.example.fitnessgym_mg.exception.EntityNotFoundException("指定された店舗IDの一部が見つかりません。");
		}
		
		return stores;
	}

	/**
	 * 店長ロールのバリデーション（共通ロジック）
	 * 
	 * @param role ユーザーロール
	 * @param storeIds 店舗IDのセット
	 */
	private void validateManagerRole(UserRole role, Set<UUID> storeIds) {
		if (role == UserRole.MANAGER) {
			// 店長の場合、店舗IDが1つだけ存在することを確認
			if (storeIds == null || storeIds.size() != 1) {
				throw new IllegalArgumentException("店長ユーザーには、割り当てる店舗を一つだけ選択する必要があります。");
			}
		}
	}

	/**
	 * マネージャー権限チェック（共通ロジック）
	 * マネージャーはトレーナーのみ編集可能
	 * 
	 * @param targetRole 対象ユーザーのロール
	 */
	private void validateManagerPermission(UserRole targetRole) {
		if (securityUtil.isManager() && targetRole != UserRole.TRAINER) {
			throw new IllegalArgumentException("マネージャーはトレーナーのみ編集可能です。");
		}
	}

	/**
	 * マネージャー権限チェック（オーバーロード）
	 * 編集前と編集後の両方のロールをチェック
	 * 
	 * @param currentRole 現在のロール
	 * @param newRole 新しいロール
	 */
	private void validateManagerPermission(UserRole currentRole, UserRole newRole) {
		validateManagerPermission(currentRole);
		validateManagerPermission(newRole);
	}
}