package com.example.fitnessgym_mg.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.fitnessgym_mg.dto.response.LessonResponse;
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.service.LessonService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class LessonController {

	private final LessonService lessonService;
	private final CustomerRepository customerRepository;

	@GetMapping("/lessons/{customerId}")
	public String showLessonHistory(@PathVariable Long customerId, Model model) {
		Customer customer = customerRepository.findById(customerId).orElseThrow();
		List<LessonResponse> lessons = lessonService.getLessonHistory(customer);

		model.addAttribute("customer", customer);
		model.addAttribute("lessons", lessons);
		return "history";
	}
}