package com.example.merging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MergingApplication {

	public static void main(String[] args) {
		SpringApplication.run(MergingApplication.class, args);
	}

}
