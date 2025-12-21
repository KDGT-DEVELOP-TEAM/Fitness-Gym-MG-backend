package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.fitnessgym_mg.entity.PostureImage;
import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;

public interface PostureImageRepository extends JpaRepository<PostureImage, UUID> {

    @Query("SELECT pi FROM PostureImage pi WHERE pi.postureGroup.id = :postureGroupId ORDER BY pi.takenAt ASC")
    List<PostureImage> findByPostureGroupIdOrderByTakenAtAsc(@Param("postureGroupId") UUID postureGroupId);
    
    @Query("SELECT pi FROM PostureImage pi WHERE pi.postureGroup.id = :postureGroupId AND pi.position = :position")
    Optional<PostureImage> findByPostureGroupIdAndPosition(@Param("postureGroupId") UUID postureGroupId, @Param("position") PostureImagePosition position);
    
    List<PostureImage> findAllByIdIn(List<UUID> ids);
}

