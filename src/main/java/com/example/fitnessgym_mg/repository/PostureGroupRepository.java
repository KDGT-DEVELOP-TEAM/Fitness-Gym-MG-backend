package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.PostureGroup;

@Repository
public interface PostureGroupRepository extends JpaRepository<PostureGroup, UUID> {
    
    List<PostureGroup> findByLessonIdOrderByCapturedAtDesc(UUID lessonId);
    
    List<PostureGroup> findByCustomerIdOrderByCapturedAtDesc(UUID customerId);
}

