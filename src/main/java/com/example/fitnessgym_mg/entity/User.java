package com.example.fitnessgym_mg.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.converter.UserRoleConverter;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DB usersテーブルとマッピングするエンティティ
 * システムユーザー（トレーナー、マネージャー、管理者）を表す
 * DBスキーマに合わせてkanaとcreatedAtフィールドを追加
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // メールアドレス（DB側でCHECK制約あり）
    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    // フリガナ（DBスキーマに合わせて追加）
    @Column(name = "kana", nullable = false)
    private String kana;

    @Column(name = "name", nullable = false)
    private String name;

    // bcryptハッシュ化されたパスワード
    @Column(name = "pass", length = 60, nullable = false)
    private String passwordHash;

    @Convert(converter = UserRoleConverter.class)
    @Column(name = "role", length = 20, nullable = false)
    private UserRole role;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    // ユーザー登録日時（DBスキーマに合わせて追加）
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

