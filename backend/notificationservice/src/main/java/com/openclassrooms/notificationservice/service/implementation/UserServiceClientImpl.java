package com.openclassrooms.notificationservice.service.implementation;

import com.openclassrooms.notificationservice.domain.Response;
import com.openclassrooms.notificationservice.dtorequest.UserRequest;
import com.openclassrooms.notificationservice.exception.ApiException;
import com.openclassrooms.notificationservice.service.UserServiceClient;
import com.openclassrooms.notificationservice.utils.RequestUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceClientImpl implements UserServiceClient {

    private final WebClient authServerWebClient;
    private static final String CIRCUIT_BREAKER_NAME = "authServerService";
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    @Override
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserByUuidFallback")
    public Mono<UserRequest> getUserByUuid(String userUuid) {
        log.debug("Fetching user by UUID: {}", userUuid);

        return authServerWebClient.get()
                .uri("/user/{uuid}", userUuid)
                .retrieve()
                .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), response -> {log.warn("User not found: {}", userUuid);return Mono.empty();})
                .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(new ApiException("Erreur client lors de la récupération de l'utilisateur")))
                .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new ApiException("Erreur serveur Authorization Server")))
                .bodyToMono(Response.class)
                .filter(response -> response != null && response.data() != null && response.data().containsKey("user"))
                .map(response -> RequestUtils.convertResponse(response, UserRequest.class, "user"))
                .doOnSuccess(user -> {if (user != null) log.debug("User found: {}", userUuid);})
                .timeout(TIMEOUT);
    }

    @Override
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserByEmailFallback")
    public Mono<UserRequest> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);

        return authServerWebClient.get()
                .uri("/user/user/{email}", email)
                .retrieve()
                .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), response -> {log.debug("User not found for email: {}", email);return Mono.empty();})
                .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(new ApiException("Erreur client lors de la récupération de l'utilisateur")))
                .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new ApiException("Erreur serveur Authorization Server")))
                .bodyToMono(Response.class)
                .filter(response -> response != null && response.data() != null && response.data().containsKey("user"))
                .map(response -> RequestUtils.convertResponse(response, UserRequest.class, "user"))
                .doOnSuccess(user -> {if (user != null) log.debug("User found for email: {}", email);})
                .timeout(TIMEOUT);
    }

    // FALLBACKS

    public Mono<UserRequest> getUserByUuidFallback(String userUuid, Throwable t) {
        log.error("Fallback getUserByUuid - UUID: {}, Cause: {}", userUuid, t.getMessage());
        return Mono.empty();
    }

    public Mono<UserRequest> getUserByEmailFallback(String email, Throwable t) {
        log.error("Fallback getUserByEmail - Email: {}, Cause: {}", email, t.getMessage());
        return Mono.empty();
    }
}