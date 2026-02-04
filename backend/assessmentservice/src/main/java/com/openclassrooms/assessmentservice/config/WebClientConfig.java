package com.openclassrooms.assessmentservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Configuration des WebClients pour la communication inter-services.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${services.patient.url:http://localhost:8081}")
    private String patientServiceUrl;

    @Value("${services.notes.url:http://localhost:8082}")
    private String notesServiceUrl;

    @Bean
    public WebClient patientWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(patientServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    @Bean
    public WebClient notesWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(notesServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("Request: {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.debug("Response status: {}", response.statusCode());
            return Mono.just(response);
        });
    }
}