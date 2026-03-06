package com.openclassrooms.assessmentservice.config;

import com.openclassrooms.assessmentservice.handler.WebClientInterceptor;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration WebClient pour la communication inter-services.
 * Utilise des URLs directes par hostname Docker (pas de load balancer Eureka).
 *
 * @author Kardigué MAGASSA
 * @version 3.0
 * @since 2026-03-03
 */
@Slf4j
@Configuration
public class WebClientConfig {


    @Value("${services.patient.url}")
    private String patientServiceUrl;

    @Value("${services.notes.url}")
    private String notesServiceUrl;

    @Value("${services.timeout:5000}")
    private int defaultTimeout;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(WebClientInterceptor.jwtAuthorizationFilter())
                .filter(WebClientInterceptor.logRequest())
                .filter(WebClientInterceptor.logResponse())
                .filter(WebClientInterceptor.handleError());
    }

    @Bean("patientServiceWebClient")
    public WebClient patientServiceWebClient(WebClient.Builder webClientBuilder) {
        log.info("Configuring WebClient for PatientService: {}", patientServiceUrl);
        return webClientBuilder.clone()
                .baseUrl(patientServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(defaultTimeout)))
                .build();
    }

    @Bean("notesServiceWebClient")
    public WebClient notesServiceWebClient(WebClient.Builder webClientBuilder) {
        log.info("Configuring WebClient for NotesService: {}", notesServiceUrl);
        return webClientBuilder.clone()
                .baseUrl(notesServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(defaultTimeout)))
                .build();
    }

    private HttpClient createHttpClient(int timeout) {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .responseTimeout(Duration.ofMillis(timeout))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout, TimeUnit.MILLISECONDS)));
    }
}