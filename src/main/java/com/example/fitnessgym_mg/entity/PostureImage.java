package com.example.fitnessgym_mg.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.converter.PostureImagePositionConverter;
import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DB posture_imagesテーブルとマッピングするエンティティ
 * 姿勢画像を表す
 */
@Entity
@Table(name = "posture_images")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 姿勢画像グループ（必須関連）
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posture_group_id", nullable = false)
    private PostureGroup postureGroup;

    // ストレージ上のパス
    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    // 公開同意フラグ
    @Column(name = "consent_publication", nullable = false)
    private boolean consentPublication;

    // 撮影日時
    @Column(name = "taken_at", nullable = false)
    private OffsetDateTime takenAt;

    // 撮影方向（Enum型）
    @Convert(converter = PostureImagePositionConverter.class)
    @Column(name = "position", nullable = false)
    private PostureImagePosition position;

    // 作成日時
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
