package com.example.fitnessgym_mg.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * DB posture_groups テーブルとマッピングするエンティティ
 * 姿勢画像グループを表す
 */
@Entity
@Table(name = "posture_groups", 
       uniqueConstraints = @UniqueConstraint(columnNames = { "lesson_id" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "customer", "lesson", "images" })
public class PostureGroup {

	@EqualsAndHashCode.Include
	@Id
	private UUID id = UUID.randomUUID();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lesson_id", nullable = false)
	private Lesson lesson;

	/** 撮影日時 */
	@Column(name = "captured_at", nullable = false)
	private OffsetDateTime capturedAt;

	/** 作成日時（DB登録時に自動設定） */
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@OneToMany(mappedBy = "postureGroup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<PostureImage> images = new ArrayList<>();

	/**
	 * 永続化前に作成日時を自動設定
	 * 既に値がある場合は上書きしない
	 */
	@PrePersist
	public void onPrePersist() {
		if (this.createdAt == null) {
			this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
		}
	}

	/**
	 * 画像追加（双方向関連を同期）
	 */
	public void addImage(PostureImage image) {
		if (image == null)
			return;
		images.add(image);
		image.setPostureGroup(this);
	}

	/**
	 * 画像削除（双方向関連を同期）
	 */
	public void removeImage(PostureImage image) {
		if (image == null)
			return;
		images.remove(image);
		image.setPostureGroup(null);
	}

	/**
	 * Factory Method
	 * Builderより安全に必須項目を強制する
	 */
	public static PostureGroup create(
			Customer customer,
			Lesson lesson,
			OffsetDateTime capturedAt) {
		if (customer == null || lesson == null || capturedAt == null) {
			throw new IllegalArgumentException("PostureGroupの必須項目が不足しています");
		}

		PostureGroup group = new PostureGroup();
		group.customer = customer;
		group.lesson = lesson;
		group.capturedAt = capturedAt;
		return group;
	}
}
