package com.dataloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AssignmentTaskApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssignmentTaskApplication.class, args);
	}

}
