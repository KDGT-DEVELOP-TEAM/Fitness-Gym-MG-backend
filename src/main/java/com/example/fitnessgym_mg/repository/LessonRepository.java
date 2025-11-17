package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fitnessgym_mg.entity.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findByCustomerIdOrderByStartDateDesc(UUID customerId);
}

