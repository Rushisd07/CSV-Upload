package com.dataloader.controller;

import com.dataloader.dto.ApiResponse;
import com.dataloader.model.Customer;
import com.dataloader.model.Order;
import com.dataloader.model.Product;
import com.dataloader.repository.CategoryRepository;
import com.dataloader.repository.CustomerRepository;
import com.dataloader.repository.OrderRepository;
import com.dataloader.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class DataQueryController {

	private final CustomerRepository customerRepository;
	private final ProductRepository productRepository;
	private final OrderRepository orderRepository;
	private final CategoryRepository categoryRepository;

	@GetMapping("/customers")
	public ResponseEntity<ApiResponse<Page<Customer>>> listCustomers(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("id"));
		Page<Customer> result = customerRepository.findAll(pageable);
		return ResponseEntity.ok(ApiResponse.success("Customers retrieved", result));
	}

	@GetMapping("/products")
	public ResponseEntity<ApiResponse<Page<Product>>> listProducts(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("id"));
		Page<Product> result = productRepository.findAll(pageable);
		return ResponseEntity.ok(ApiResponse.success("Products retrieved", result));
	}

	@GetMapping("/orders")
	public ResponseEntity<ApiResponse<Page<Order>>> listOrders(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("id"));
		Page<Order> result = orderRepository.findAll(pageable);
		return ResponseEntity.ok(ApiResponse.success("Orders retrieved", result));
	}

	@GetMapping("/categories")
	public ResponseEntity<ApiResponse<?>> listCategories() {
		return ResponseEntity.ok(ApiResponse.success("Categories retrieved", categoryRepository.findAll()));
	}

	@GetMapping("/summary")
	public ResponseEntity<ApiResponse<Map<String, Long>>> getSummary() {
		Map<String, Long> summary = Map.of("totalCustomers", customerRepository.count(), "totalProducts",
				productRepository.count(), "totalOrders", orderRepository.count(), "totalCategories",
				categoryRepository.count());
		return ResponseEntity.ok(ApiResponse.success("Data summary", summary));
	}
}
