package com.example.springapp;

import com.example.springapp.user.UserEntity;
import com.example.springapp.user.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringappApplication implements CommandLineRunner {

	private final UserService userService;

	public SpringappApplication(UserService userService) {
		this.userService = userService;
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringappApplication.class, args);
	}


	@Override
	public void run(String... args) {
		UserEntity defaultUser = new UserEntity();
		defaultUser.setPassword("1234");
		defaultUser.setEmail("default@outlook.com");
		userService.register(defaultUser);
	}
}
