package com.example.fitnessgym_mg.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.service.CustomerService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService customerService;

	@GetMapping("/customers")
	public String showCustomerList(Model model) {
		List<Customer> recentCustomers = customerService.getRecentCustomers(7);

		model.addAttribute("customers", recentCustomers);
		model.addAttribute("hasCustomers", !recentCustomers.isEmpty());
		return "customer-selection";
	}
}