package com.example.fitnessgym_mg.repository;

import org.springframework.data.repository.CrudRepository;
import com.example.fitnessgym_mg.entity.Lesson;

public interface LessonRepository extends CrudRepository<Lesson, Long> {
}

