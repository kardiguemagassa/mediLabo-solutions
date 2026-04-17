package com.openclassrooms.notificationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration minimal pour vérifier que le contexte JDBC se charge correctement.
 * Utilise @DataJdbcTest au lieu de @SpringBootTest pour éviter de charger
 * les beans Email/Kafka/OAuth2 qui nécessitent une configuration externe.
 *
 * @author Kardigué MAGASSA
 */
@DataJdbcTest
@ActiveProfiles("test")
@Testcontainers
class NotificationserviceApplicationTests {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
			.withDatabaseName("notificationdb_test")
			.withUsername("test")
			.withPassword("test");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Test
	void contextLoads() {}
}