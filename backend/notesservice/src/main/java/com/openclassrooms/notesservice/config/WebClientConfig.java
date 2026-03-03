package com.openclassrooms.notesservice.config;

import com.openclassrooms.notesservice.handler.WebClientInterceptor;
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
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${PATIENT_SERVICE_URL:http://localhost:8081}")
    private String patientServiceUrl;

    @Value("${services.patient-service.timeout:5000}")
    private int patientServiceTimeout;

    /**
     * WebClient pour communiquer avec PatientService.
     */
    @Bean("patientServiceWebClient")
    public WebClient patientServiceWebClient(WebClient.Builder builder) {
        log.info("Configuring WebClient for PatientService: {}", patientServiceUrl);

        return builder
                .baseUrl(patientServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(patientServiceTimeout)))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(WebClientInterceptor.jwtAuthorizationFilter())
                .filter(WebClientInterceptor.logRequest())
                .filter(WebClientInterceptor.logResponse())
                .filter(WebClientInterceptor.handleError())
                .build();
    }

    /**
     * Configuration HTTP avec timeouts.
     */
    private HttpClient createHttpClient(int timeout) {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .responseTimeout(Duration.ofMillis(timeout))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout, TimeUnit.MILLISECONDS)));
    }
}