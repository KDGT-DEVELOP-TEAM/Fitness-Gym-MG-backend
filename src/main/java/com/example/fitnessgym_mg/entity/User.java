package com.example.fitnessgym_mg.entity;

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

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "pass", length = 60, nullable = false)
    private String passwordHash;

    @Convert(converter = UserRoleConverter.class)
    @Column(name = "role", length = 20, nullable = false)
    private UserRole role;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}

