package com.example.fitnessgym_mg.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
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
 * DB lessonsテーブルとマッピングするエンティティ
 * 顧客が受けたトレーニングレッスンの記録を表す
 * 関連エンティティはLAZY読み込みで必要時のみDBから取得
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // レッスン実施店舗（LAZY: アクセス時にDBから取得）
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id")
    private Store store;

    // 担当トレーナー（LAZY: アクセス時にDBから取得）
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User trainer;

    // レッスン受講顧客（LAZY: アクセス時にDBから取得）
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "condition", length = 50)
    private String condition;

    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "meal", length = 150)
    private String meal;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "start_date")
    private OffsetDateTime startDate;

    @Column(name = "end_date")
    private OffsetDateTime endDate;

    @Column(name = "next_date")
    private OffsetDateTime nextDate;

    // 次回予定店舗（任意）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_store_id")
    private Store nextStore;

    // 次回担当トレーナー（任意）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_user_id")
    private User nextTrainer;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}

