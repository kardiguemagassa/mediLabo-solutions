package com.openclassrooms.gatewayserverservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Application principale du Gateway Server REACTIVE pour MediLabo Solutions.
 * Configuration propre avec Spring Boot 3.3.x + Spring Cloud 2023.0.x :
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayserverserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayserverserviceApplication.class, args);
	}
}