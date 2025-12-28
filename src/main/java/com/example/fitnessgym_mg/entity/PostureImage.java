package com.example.fitnessgym_mg.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.example.fitnessgym_mg.entity.converter.PostureImagePositionConverter;
import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DB posture_imagesテーブルとマッピングするエンティティ
 * 姿勢画像を表す
 */
@Entity
@Table(name = "posture_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "postureGroup" })
public class PostureImage {

	@EqualsAndHashCode.Include
	@Id
	@Default
	private UUID id = UUID.randomUUID();

	/**
	 * 姿勢グループ（必須リレーション）
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "posture_group_id", nullable = false)
	private PostureGroup postureGroup;

	/**
	 * ストレージキー（Supabase Storage内のパス、ユニーク制約あり）
	 */
	@Column(name = "storage_key", nullable = false, unique = true, length = 500)
	private String storageKey;

	/**
	 * 公開同意フラグ
	 */
	@Column(name = "consent_publication", nullable = false)
	private boolean consentPublication;

	/**
	 * 撮影日時
	 */
	@Column(name = "taken_at", nullable = false)
	private OffsetDateTime takenAt;

	/**
	 * 撮影位置（FRONT, RIGHT, BACK, LEFT）
	 */
	@Convert(converter = PostureImagePositionConverter.class)
	@Column(name = "position", nullable = false)
	private PostureImagePosition position;

	/**
	 * 作成日時（DB登録時に自動設定、更新不可）
	 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@PrePersist
	public void onPrePersist() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now(ZoneOffset.UTC);
		}
	}
}