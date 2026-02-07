package com.openclassrooms.notificationservice.client;

import com.openclassrooms.notificationservice.dto.UserInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Client pour communiquer avec l'Authorization Server.
 * Utilise Resilience4j pour Circuit Breaker, Retry et TimeLimiter.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServerClient {

    private final WebClient authServerWebClient;

    private static final String CIRCUIT_BREAKER_NAME = "authServerService";

    /**
     * Récupère les informations d'un utilisateur par son email.
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserByEmailFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @TimeLimiter(name = CIRCUIT_BREAKER_NAME)
    public CompletableFuture<Optional<UserInfo>> getUserByEmail(String email) {
        log.info("Calling Auth Server for email: {}", email);

        return CompletableFuture.supplyAsync(() -> {
            UserInfo userInfo = authServerWebClient
                    .get()
                    .uri("/api/users/email/{email}", email)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND,
                            clientResponse -> Mono.empty())
                    .bodyToMono(UserInfo.class)
                    .block();

            if (userInfo == null) {
                log.warn("User not found: {}", email);
                return Optional.<UserInfo>empty();
            }

            log.info("User retrieved successfully: {} {}", userInfo.getFirstName(), userInfo.getLastName());
            return Optional.of(userInfo);
        });
    }

    /**
     * Récupère les informations d'un utilisateur par son UUID.
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserByUuidFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @TimeLimiter(name = CIRCUIT_BREAKER_NAME)
    public CompletableFuture<Optional<UserInfo>> getUserByUuid(String userUuid) {
        log.info("Calling Auth Server for UUID: {}", userUuid);

        return CompletableFuture.supplyAsync(() -> {
            UserInfo userInfo = authServerWebClient
                    .get()
                    .uri("/api/users/{uuid}", userUuid)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND,
                            clientResponse -> Mono.empty())
                    .bodyToMono(UserInfo.class)
                    .block();

            if (userInfo == null) {
                log.warn("User not found: {}", userUuid);
                return Optional.<UserInfo>empty();
            }

            log.info("User retrieved successfully: {} {}", userInfo.getFirstName(), userInfo.getLastName());
            return Optional.of(userInfo);
        });
    }

    // FALLBACK METHODS

    public CompletableFuture<Optional<UserInfo>> getUserByEmailFallback(String email, Throwable throwable) {
        log.error("Fallback triggered for Auth Server (email: {}): {}", email, throwable.getMessage());
        return CompletableFuture.completedFuture(Optional.empty());
    }

    public CompletableFuture<Optional<UserInfo>> getUserByUuidFallback(String userUuid, Throwable throwable) {
        log.error("Fallback triggered for Auth Server (uuid: {}): {}", userUuid, throwable.getMessage());
        return CompletableFuture.completedFuture(Optional.empty());
    }
}