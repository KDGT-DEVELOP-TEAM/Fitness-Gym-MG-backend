package com.example.fitnessgym_mg.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.converter.GenderConverter;
import com.example.fitnessgym_mg.entity.enums.Gender;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DB customersテーブルとマッピングするエンティティ
 * ジムの顧客情報を表す
 * CascadeType.ALL: PostureGroupsを自動保存・削除
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "kana", length = 100, nullable = false)
    private String kana;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Convert(converter = GenderConverter.class)
    @Column(name = "gender", length = 10, nullable = false)
    private Gender gender;

    @Column(name = "birthday", nullable = false)
    private LocalDate birthday;

    @Column(name = "height", precision = 5, scale = 2)
    private BigDecimal height;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "phone", length = 12, nullable = false)
    private String phone;

    @Column(name = "address", length = 200, nullable = false)
    private String address;

    @Column(name = "medical", length = 100)
    private String medical;

    @Column(name = "taboo", length = 100)
    private String taboo;

    // 初回姿勢画像グループへの参照（任意）
    @Column(name = "first_posture_group_id")
    private UUID firstPostureGroupId;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    // この顧客の姿勢画像グループリスト
    // CascadeType.ALL: Customer削除時にPostureGroupsもDBから削除
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PostureGroup> postureGroups = new ArrayList<>();
}

