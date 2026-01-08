package com.openclassrooms.discoveryserverservice;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * DiscoveryserverserviceApplication
 * @author FirstName LastName
 * @version 1.0
 * @email magassa***REMOVED_USER***@gmail.com
 * @since 2026-05-01
 */


@EnableEurekaServer
@SpringBootApplication
public class DiscoveryserverserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscoveryserverserviceApplication.class, args);
	}

	/*@Bean
	public CommandLineRunner startup(BCryptPasswordEncoder encoder) {
		return args -> {
			var password = encoder.encode("***REMOVED_USER***");
			System.out.println(password);
		};

	}*/
}
