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

@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${services.patient-service.url:http://localhost:8081}")
    private String patientServiceUrl;

    @Value("${services.patient-service.timeout:5000}")
    private int patientServiceTimeout;

    @Value("${services.authorization-server.url:http://localhost:9001}")
    private String authorizationServerUrl;

//    @Bean
//    public WebClient patientWebClient(WebClient.Builder builder) {
//        return builder
//                .baseUrl(patientServiceUrl)
//                .filter(WebClientInterceptor.jwtAuthorizationFilter())
//                .filter(WebClientInterceptor.logRequest())
//                .filter(WebClientInterceptor.logResponse())
//                .build();
//    }

    /**
     * Builder de base partagé pour éviter de répéter les filtres.
     * On injecte ici les intercepteurs de ta classe WebClientInterceptor.
     */
    @Bean
    public WebClient.Builder commonWebClientBuilder() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // Appel de tes intercepteurs personnalisés
                .filter(WebClientInterceptor.jwtAuthorizationFilter())
                .filter(WebClientInterceptor.logRequest())
                .filter(WebClientInterceptor.logResponse());
    }

    /**
     * WebClient pour PatientService.
     */
    @Bean
    public WebClient patientServiceWebClient(WebClient.Builder commonBuilder) {
        log.info("Configuring WebClient for PatientService: {}", patientServiceUrl);

        return commonBuilder.clone() // Clone pour garder les filtres de base
                .baseUrl(patientServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(patientServiceTimeout)))
                .build();
    }

    /**
     * WebClient pour Authorization Server.
     */
    @Bean
    public WebClient authServerWebClient(WebClient.Builder commonBuilder) {
        log.info("Configuring WebClient for Auth Server: {}", authorizationServerUrl);

        return commonBuilder.clone()
                .baseUrl(authorizationServerUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient(5000)))
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