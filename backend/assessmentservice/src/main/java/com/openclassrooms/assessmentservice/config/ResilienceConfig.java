package com.openclassrooms.assessmentservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration Resilience4j pour la résilience des appels inter-services.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)                    // Ouvre si 50% d'échecs
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Attend 30s avant de réessayer
                .slidingWindowSize(10)                       // Fenêtre de 10 appels
                .minimumNumberOfCalls(5)                     // Minimum 5 appels avant évaluation
                .permittedNumberOfCallsInHalfOpenState(3)    // 3 appels en semi-ouvert
                .build();
    }

    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))      // Timeout de 5 secondes
                .build();
    }
}