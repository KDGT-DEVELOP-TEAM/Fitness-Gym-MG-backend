package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.fitnessgym_mg.entity.PostureGroup;

public interface PostureGroupRepository extends JpaRepository<PostureGroup, UUID> {

    @Query("""
            SELECT DISTINCT pg
            FROM PostureGroup pg
            JOIN FETCH pg.customer c
            LEFT JOIN FETCH pg.images imgs
            JOIN FETCH pg.lesson l
            WHERE c.id = :customerId
            ORDER BY l.startDate DESC, pg.capturedAt DESC
            """)
    List<PostureGroup> findAllWithImagesByCustomerId(@Param("customerId") UUID customerId);
}

