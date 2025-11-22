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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DB posture_groupsテーブルとマッピングするエンティティ
 * 1回のレッスンで撮影した姿勢画像のグループを表す
 * CascadeType.ALL: 子のPostureImageを自動的に保存・削除
 * orphanRemoval: このグループから削除された画像をDBからも削除
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posture_groups")
public class PostureGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // 姿勢画像リスト（撮影日時の昇順で取得）
    // CascadeType.ALL: このグループ保存時に子画像も自動保存、削除時も自動削除
    @OneToMany(mappedBy = "postureGroup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("takenAt ASC")
    @Builder.Default
    private List<PostureImage> images = new ArrayList<>();
}

