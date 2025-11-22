package com.example.fitnessgym_mg.service;

import java.util.Set;
import java.util.UUID;

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
import com.example.fitnessgym_mg.entity.User.UserRole;
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.repository.StoreRepository;
import com.example.fitnessgym_mg.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

	private final UserRepository userRepository;
	private final StoreRepository storeRepository;
	private final PasswordEncoder passwordEncoder;
	private final LessonRepository lessonRepository;

	// --- ユーザー検索（storeIdによる中間テーブル経由の絞り込み対応） ---
	@Transactional(readOnly = true)
	public Page<UserResponse> searchUsers(
			String keyword,
			String role,
			String sort,
			UUID storeId,
			Pageable pageable) {

		// 1. Specificationの構築
		Specification<User> spec = (root, query, cb) -> null;

		// --- 1-1. 店舗IDによる絞り込み (中間テーブル Store_Users 経由) ---
		if (storeId != null) {
			// User -> Store の結合を行い、Store IDが一致するユーザーに絞り込みます。
			// Root(User).join("stores").get("id") が storeId と equal である
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
				// 不正なロールは無視
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
	public void create(UserRequest req, UUID storeId) {

		// 1. 店長ロールのバリデーション (単一の storeId チェック)
		if (req.getRole() == UserRole.manager) {
			if (storeId == null) {
				throw new IllegalArgumentException("店長ユーザーには、割り当てる店舗を一つ選択する必要があります。");
			}
		}

		// 2. ユーザーの基本情報設定
		User user = new User();
		user.setEmail(req.getEmail());
		user.setName(req.getName());
		user.setKana(req.getKana());
		user.setRole(req.getRole());
		user.setActive(req.isActive());
		user.setPass(passwordEncoder.encode(req.getPass()));

		// 3. 店舗の紐づけ
		if (storeId != null) {
			// 単一のStoreエンティティを取得
			Store store = storeRepository.findById(storeId)
					.orElseThrow(() -> new RuntimeException("指定された店舗IDが見つかりません: " + storeId));

			// Userエンティティのstoresコレクションに、この単一のStoreをセット
			user.setStores(Set.of(store));
		}

		userRepository.save(user);
	}

	// 更新（権限チェックを追加）
	public void update(UUID id, UserRequest req, UUID storeId) {
		User user = findUserById(id, storeId); // 権限チェック付き取得

		// 1. 店長ロールのバリデーション (単一の storeId チェック)
		if (req.getRole() == UserRole.manager) {
			// 更新後のロールがMANAGERの場合、紐づけ情報 (storeId) が必須
			if (storeId == null) {
				throw new IllegalArgumentException("店長ユーザーには、割り当てる店舗を一つ選択する必要があります。");
			}
		}

		// 既存の更新ロジック
		user.setEmail(req.getEmail());
		user.setName(req.getName());
		user.setKana(req.getKana());
		user.setRole(req.getRole());
		user.setActive(req.isActive());

		if (req.getPass() != null && !req.getPass().isEmpty()) {
			user.setPass(passwordEncoder.encode(req.getPass()));
		}

		// 2. 店舗の紐づけ情報の上書き
		if (storeId != null) {
			Store store = storeRepository.findById(storeId)
					.orElseThrow(() -> new RuntimeException("指定された店舗IDが見つかりません: " + storeId));
			user.setStores(Set.of(store));
		} else {
			// storeId が null で、かつロールが ADMIN/TRAINER の場合、紐づけを解除する
			user.setStores(Set.of());
		}

		userRepository.save(user);
	}

	// --- ユーザーを有効化 ---
	public void enableActive(UUID id, UUID storeId) {
		User user = findUserById(id, storeId); // 権限チェック付き取得

		// 既に有効ならスキップ（あるいは処理続行）
		if (user.isActive()) {
			return; // 既に有効
		}

		user.setActive(true);
		userRepository.save(user);
	}

	// --- ユーザーを無効化 ---
	public void disableActive(UUID id, UUID storeId) {
		User user = findUserById(id, storeId); // 権限チェック付き取得

		// 既に無効ならスキップ
		if (!user.isActive()) {
			return; // 既に無効
		}

		user.setActive(false);
		userRepository.save(user);
	}

	// 削除（権限チェックを追加）
	public void delete(UUID id, UUID storeId) {
		User user = findUserById(id, storeId); // 権限チェック付き取得

		// 有効ユーザーは削除不可
		if (user.isActive()) {
			throw new IllegalStateException("有効ユーザーは削除できません");
		}

		if (hasRelatedData(id)) {
			// 関連データが存在する場合、例外をスローして削除を拒否
			throw new IllegalStateException("このユーザーにはレッスン履歴が紐づいているため、削除できません。無効化してください。");
		}

		userRepository.delete(user);
	}

	// idでアカウント情報を取得（権限チェックを追加）
	@Transactional(readOnly = true)
	public UserResponse findById(UUID id, UUID storeId) {
		User user = findUserById(id, storeId);
		return UserResponse.fromEntity(user);
	}

	// --- ヘルパーメソッド ---

	// IDでUserエンティティを取得し、storeIdが提供されていれば中間テーブル経由の権限チェックを行う
	private User findUserById(UUID id, UUID storeId) {
		User user = userRepository.findById(id)
				// ★ RuntimeExceptionに置き換え ★
				.orElseThrow(() -> new RuntimeException("User not found with id: " + id));

		// 店長の場合 (storeId != null)、操作対象のユーザーが自分の店舗に属するかチェック
		if (storeId != null) {
			// Userが持つ stores コレクションに、該当 storeId が存在するか確認
			boolean isAssignedToStore = user.getStores().stream()
					.anyMatch(store -> store.getId().equals(storeId));

			if (!isAssignedToStore) {
				// 権限外のユーザーへの操作は拒否
				throw new RuntimeException("User not found (or access denied) with id: " + id);
			}
		}
		return user;
	}

	// 複合ソートを生成するヘルパーメソッド（ロール順序 + 登録日時降順）
	private Sort createSort(String sort) {
		// 1. ロール順序による昇順ソート
		Sort primarySort = Sort.by("roleOrder").ascending();

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

	// --- ヘルパーメソッド: 関連データの存在チェック ---
	/**
	 * 指定されたユーザーIDに関連するデータ（レッスン履歴）が存在するか確認する
	 */
	private boolean hasRelatedData(UUID userId) {
		// レッスンデータとの紐づきチェック
		return lessonRepository.countByUserId(userId) > 0;
	}
}