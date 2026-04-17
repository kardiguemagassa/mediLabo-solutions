package com.openclassrooms.gatewayserverservice.exception;

import com.openclassrooms.gatewayserverservice.domain.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;


@WebFluxTest
@ContextConfiguration(classes = {HandleException.class, HandleExceptionTest.TestController.class})
class HandleExceptionTest {

    @Autowired
    private WebTestClient webTestClient;

    @RestController
    static class TestController {
        @GetMapping("/throw-api")
        public Mono<Void> throwApi() {
            return Mono.error(new ApiException("Erreur API spécifique"));
        }

        @GetMapping("/throw-bad-credentials")
        public Mono<Void> throwBadCredentials() {
            return Mono.error(new BadCredentialsException("Auth failed"));
        }

        @GetMapping("/throw-access-denied")
        public Mono<Void> throwAccessDenied() {
            return Mono.error(new AccessDeniedException("Forbidden access"));
        }

        @GetMapping("/throw-global")
        public Mono<Void> throwGlobal() {
            return Mono.error(new RuntimeException("Crash total"));
        }
    }

    @Test
    @WithMockUser
    void handleApiException() {
        webTestClient.get().uri("/throw-api").exchange()
                .expectStatus().isBadRequest()
                .expectBody(Response.class).value(res -> {
                    assertEquals("Erreur API spécifique", res.message());
                    assertEquals(400, res.code());
                });
    }

    @Test
    @WithMockUser
    void handleBadCredentials() {
        webTestClient.get().uri("/throw-bad-credentials").exchange()
                .expectStatus().isUnauthorized()
                .expectBody(Response.class).value(res -> {
                    assertEquals("Identifiants incorrects.", res.message());
                    assertEquals(401, res.code());
                });
    }

    @Test
    @WithMockUser
    void handleAccessDenied() {
        webTestClient.get().uri("/throw-access-denied").exchange()
                .expectStatus().isForbidden()
                .expectBody(Response.class).value(res -> {
                    assertEquals("Accès refusé. Vous n'avez pas les permissions nécessaires.", res.message());
                    assertEquals(403, res.code());
                });
    }

    @Test
    @WithMockUser
    void handleGlobalException() {
        webTestClient.get().uri("/throw-global").exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Response.class).value(res -> {
                    assertEquals("Une erreur interne s'est produite au niveau du Gateway.", res.message());
                    assertEquals(500, res.code());
                });
    }
}