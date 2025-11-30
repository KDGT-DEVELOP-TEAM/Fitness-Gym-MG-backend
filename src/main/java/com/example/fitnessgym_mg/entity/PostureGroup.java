package com.example.fitnessgym_mg.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DB posture_groupsテーブルとマッピングするエンティティ
 * 姿勢画像グループを表す
 */
@Entity
@Table(name = "posture_groups")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostureGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 顧客（必須関連）
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // レッスン（必須関連）
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    // 撮影日時
    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    // 作成日時
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 姿勢画像リスト（一対多）
    @OneToMany(mappedBy = "postureGroup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PostureImage> images = new ArrayList<>();
}
