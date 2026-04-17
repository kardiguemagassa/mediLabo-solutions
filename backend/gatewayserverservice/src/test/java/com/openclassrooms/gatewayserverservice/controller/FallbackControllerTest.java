package com.openclassrooms.gatewayserverservice.controller;

import com.openclassrooms.gatewayserverservice.domain.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WebFluxTest(FallbackController.class)
class FallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @WithMockUser // Simule un utilisateur authentifié pour passer la barrière 401
    void patientFallback_ShouldReturnServiceUnavailable() {
        webTestClient.get()
                .uri("/fallback/patients")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(Response.class)
                .value(response -> {
                    assertEquals(503, response.status().value()); // Note: .value() pour l'entier
                    assertEquals("Le service Patient est momentanément indisponible.", response.message());
                });
    }

    @Test
    @WithMockUser
    void notesFallback_ShouldReturnServiceUnavailable() {
        webTestClient.get()
                .uri("/fallback/notes")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(Response.class)
                .value(response -> {
                    assertEquals("Le service des Notes est indisponible pour le moment.", response.message());
                });
    }

    @Test
    @WithMockUser
    void assessmentFallback_ShouldReturnServiceUnavailable() {
        webTestClient.get()
                .uri("/fallback/assessments")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(Response.class)
                .value(response -> {
                    assertEquals("Le service d'évaluation santé est indisponible.", response.message());
                });
    }
}