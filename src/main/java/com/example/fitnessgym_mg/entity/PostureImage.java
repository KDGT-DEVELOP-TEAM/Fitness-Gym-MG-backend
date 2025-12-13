package com.example.fitnessgym_mg.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.example.fitnessgym_mg.entity.converter.PostureImagePositionConverter;
import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class PostureImage {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "posture_group_id", nullable = false)
	private PostureGroup postureGroup;

	@Column(name = "storage_key", nullable = false, unique = true)
	private String storageKey;

	@Column(name = "consent_publication", nullable = false)
	private boolean consentPublication;

	@Column(name = "taken_at", nullable = false)
	private OffsetDateTime takenAt;

	@Convert(converter = PostureImagePositionConverter.class)
	@Column(name = "position", nullable = false)
	private PostureImagePosition position;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@PrePersist
	public void prePersist() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now();
		}
	}
}