package com.example.fitnessgym_mg.repository;

import org.springframework.data.repository.CrudRepository;
import com.example.fitnessgym_mg.entity.Training;

public interface TrainingRepository extends CrudRepository<Training, Training.TrainingId> {
}

