package com.openclassrooms.patientservice.service.implementation;

import com.openclassrooms.patientservice.domain.Response;
import com.openclassrooms.patientservice.dtorequest.UserRequest;
import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.service.UserServiceClient;
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

import static com.openclassrooms.patientservice.util.RequestUtils.convertResponse;

/**
 * Implémentation du service de communication avec Authorization Server.
 * Le token JWT est automatiquement propagé via WebClientInterceptor.
 *
 * @author Kardigué MAGASSA
 * @version 2.1
 * @since 2026-01-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceClientImpl implements UserServiceClient {

    private final WebClient authServerWebClient;
    private static final String CIRCUIT_BREAKER_NAME = "authServerService";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Override
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getUserByUuidFallback")
    public Mono<UserRequest> getUserByUuid(String userUuid) {
        log.debug("Fetching user by UUID: {}", userUuid);

        return authServerWebClient.get()
                .uri("/user/{userUuid}", userUuid)
                .retrieve()
                .onStatus(status -> status.equals(HttpStatus.NOT_FOUND),
                        response -> {log.warn("User not found: {}", userUuid);return Mono.error(new ApiException("Utilisateur non trouvé: " + userUuid));})
                .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(new ApiException("Erreur client lors de la récupération de l'utilisateur")))
                .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new ApiException("Erreur serveur Authorization Server")))
                .bodyToMono(Response.class)
                .filter(response -> response != null && response.data() != null && response.data().containsKey("user"))
                .map(response -> convertResponse(response, UserRequest.class, "user"))
                .switchIfEmpty(Mono.error(new ApiException("Réponse vide du service utilisateur")))
                .doOnSuccess(user -> log.debug("User found: {}", userUuid))
                .doOnError(error -> log.error("Error fetching user {}: {}", userUuid, error.getMessage()))
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
                .map(response -> convertResponse(response, UserRequest.class, "user"))
                .doOnSuccess(user -> {if (user != null) log.debug("User found for email: {}", email);})
                .doOnError(error -> log.error("Error fetching user by email {}: {}", email, error.getMessage()))
                .timeout(TIMEOUT);
    }

    @Override
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getAssigneeFallback")
    public Mono<UserRequest> getAssignee(String patientUuid) {
        log.debug("Fetching assignee for patient: {}", patientUuid);

        return authServerWebClient.get()
                .uri("/user/assignee/{patientUuid}", patientUuid)
                .retrieve()
                .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), response -> {log.warn("Assignee not found for patient: {}", patientUuid);
                    return Mono.error(new ApiException("Assigné non trouvé pour le patient: " + patientUuid));})
                .onStatus(HttpStatusCode::is4xxClientError, response -> Mono.error(new ApiException("Erreur client lors de la récupération de l'assigné")))
                .onStatus(HttpStatusCode::is5xxServerError, response -> Mono.error(new ApiException("Erreur serveur Authorization Server")))
                .bodyToMono(Response.class)
                .filter(response -> response != null && response.data() != null && response.data().containsKey("user"))
                .map(response -> convertResponse(response, UserRequest.class, "user"))
                .switchIfEmpty(Mono.error(new ApiException("Réponse vide du service utilisateur")))
                .doOnSuccess(user -> log.debug("Assignee found for patient: {}", patientUuid))
                .doOnError(error -> log.error("Error fetching assignee for {}: {}", patientUuid, error.getMessage()))
                .timeout(TIMEOUT);
    }

    //FALLBACKS

    public Mono<UserRequest> getUserByUuidFallback(String userUuid, Throwable t) {
        log.error("Fallback getUserByUuid - UUID: {}, Cause: {}", userUuid, t.getMessage());
        return Mono.error(new ApiException("Service utilisateur indisponible"));
    }

    public Mono<UserRequest> getUserByEmailFallback(String email, Throwable t) {
        log.error("Fallback getUserByEmail - Email: {}, Cause: {}", email, t.getMessage());
        return Mono.empty();
    }

    public Mono<UserRequest> getAssigneeFallback(String patientUuid, Throwable t) {
        log.error("Fallback getAssignee - Patient: {}, Cause: {}", patientUuid, t.getMessage());
        return Mono.error(new ApiException("Service utilisateur indisponible"));
    }
}