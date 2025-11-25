package com.example.fitnessgym_mg.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.fitnessgym_mg.dto.response.LessonResponse;
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.Lesson;
import com.example.fitnessgym_mg.repository.LessonRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonService {

	private final LessonRepository lessonRepository;

	public List<LessonResponse> getLessonHistory(Customer customer) {
		List<Lesson> lessons = lessonRepository.findByCustomerOrderByReservationDateDesc(customer);
		return lessons.stream()
				.map(l -> new LessonResponse(l.getReservationDate(), l.getWeight(), l.getBmi()))
				.collect(Collectors.toList());
	}
}
