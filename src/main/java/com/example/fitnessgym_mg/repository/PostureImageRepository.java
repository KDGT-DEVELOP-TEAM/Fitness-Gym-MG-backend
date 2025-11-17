package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fitnessgym_mg.entity.PostureImage;

public interface PostureImageRepository extends JpaRepository<PostureImage, UUID> {

    List<PostureImage> findByPostureGroupIdOrderByTakenAtAsc(UUID postureGroupId);
}

