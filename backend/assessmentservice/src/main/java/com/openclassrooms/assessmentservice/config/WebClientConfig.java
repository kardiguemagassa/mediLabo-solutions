package com.openclassrooms.assessmentservice.config;

import com.openclassrooms.assessmentservice.handler.WebClientInterceptor;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
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
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${services.patient.name:PATIENTSERVICE}")
    private String patientServiceName;

    @Value("${services.notes.name:NOTESSERVICE}")
    private String notesServiceName;

    @Value("${services.timeout:5000}")
    private int defaultTimeout;

    /**
     * Builder de base partagé avec LoadBalancer.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(WebClientInterceptor.jwtAuthorizationFilter())
                .filter(WebClientInterceptor.logRequest())
                .filter(WebClientInterceptor.logResponse())
                .filter(WebClientInterceptor.handleError());
    }

    /**
     * WebClient pour PatientService.
     */
    @Bean("patientServiceWebClient")
    public WebClient patientServiceWebClient(WebClient.Builder loadBalancedWebClientBuilder) {
        String url = "http://" + patientServiceName;
        log.info("Configuring LoadBalanced WebClient for PatientService: {}", url);

        return loadBalancedWebClientBuilder.clone()
                .baseUrl(url)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(defaultTimeout)))
                .build();
    }

    /**
     * WebClient pour NotesService.
     */
    @Bean("notesServiceWebClient")
    public WebClient notesServiceWebClient(WebClient.Builder loadBalancedWebClientBuilder) {
        String url = "http://" + notesServiceName;
        log.info("Configuring LoadBalanced WebClient for NotesService: {}", url);

        return loadBalancedWebClientBuilder.clone()
                .baseUrl(url)
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