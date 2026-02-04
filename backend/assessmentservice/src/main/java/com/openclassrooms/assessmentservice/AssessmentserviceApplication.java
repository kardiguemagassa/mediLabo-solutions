package com.openclassrooms.assessmentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AssessmentserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssessmentserviceApplication.class, args);
	}

}
