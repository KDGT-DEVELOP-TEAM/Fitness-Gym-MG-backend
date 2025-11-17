package com.example.fitnessgym_mg.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;
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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posture_images")
public class PostureImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posture_group_id")
    private PostureGroup postureGroup;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "consent_publication", nullable = false)
    private boolean consentPublication;

    @Column(name = "taken_at", nullable = false)
    private OffsetDateTime takenAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "position", length = 10, nullable = false)
    private PostureImagePosition position;
}

