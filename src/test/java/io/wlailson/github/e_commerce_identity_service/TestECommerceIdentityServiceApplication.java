package io.wlailson.github.e_commerce_identity_service;

import org.springframework.boot.SpringApplication;

public class TestECommerceIdentityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(Application::main).with(TestcontainersConfiguration.class).run(args);
	}

}
