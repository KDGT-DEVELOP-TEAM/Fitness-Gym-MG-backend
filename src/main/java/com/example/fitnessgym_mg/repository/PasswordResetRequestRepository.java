package com.example.fitnessgym_mg.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.PasswordResetRequest;
import com.example.fitnessgym_mg.entity.enums.PasswordResetStatus;

/**
 * パスワードリセットリクエストリポジトリ
 */
@Repository
public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, UUID> {

	/**
	 * 未処理のリクエスト一覧を取得（リクエスト日時の降順）
	 * EntityGraphでユーザー情報も取得してN+1問題を回避
	 * 
	 * @deprecated ページネーション対応のfindByStatusOrderByRequestedAtDesc(PasswordResetStatus, Pageable)を使用してください
	 */
	@Deprecated
	@EntityGraph(attributePaths = { "user", "processedBy" })
	List<PasswordResetRequest> findByStatusOrderByRequestedAtDesc(PasswordResetStatus status);

	/**
	 * 未処理のリクエスト一覧を取得（リクエスト日時の降順、ページネーション対応）
	 * EntityGraphでユーザー情報も取得してN+1問題を回避
	 * 
	 * @param status リクエスト状態
	 * @param pageable ページネーション情報
	 * @return リクエストのページ
	 */
	@EntityGraph(attributePaths = { "user", "processedBy" })
	Page<PasswordResetRequest> findByStatusOrderByRequestedAtDesc(PasswordResetStatus status, Pageable pageable);

	/**
	 * 指定されたメールアドレスから指定日時以降に作成されたリクエストが存在するかチェック
	 * 
	 * @param email メールアドレス
	 * @param requestedAt 指定日時（この日時以降に作成されたリクエストを検索）
	 * @return 存在する場合 true
	 */
	@Query("SELECT COUNT(p) > 0 FROM PasswordResetRequest p WHERE p.email = :email AND p.requestedAt >= :requestedAt")
	boolean existsByEmailAndRequestedAtAfter(@Param("email") String email, @Param("requestedAt") OffsetDateTime requestedAt);
}
