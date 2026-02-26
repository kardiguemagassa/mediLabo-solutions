package com.openclassrooms.notificationservice;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class NotificationserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationserviceApplication.class, args);
	}

	@PostConstruct
	public void setupSecurityContext() {
		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
	}

}
