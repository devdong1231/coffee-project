package com.coffeeproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CoffeeProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoffeeProjectApplication.class, args);
	}

}
