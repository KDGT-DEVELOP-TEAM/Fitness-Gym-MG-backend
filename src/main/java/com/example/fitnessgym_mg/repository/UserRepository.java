package com.example.fitnessgym_mg.repository;

import org.springframework.data.repository.CrudRepository;
import com.example.fitnessgym_mg.entity.User;

public interface UserRepository extends CrudRepository<User, Long> {
}

