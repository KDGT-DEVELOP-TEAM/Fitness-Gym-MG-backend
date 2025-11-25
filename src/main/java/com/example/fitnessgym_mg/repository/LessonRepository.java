package com.example.fitnessgym_mg.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.Lesson;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

	List<Lesson> findByCustomerOrderByReservationDateDesc(Customer customer);
}