package com.openclassrooms.notificationservice.config;

import com.openclassrooms.notificationservice.handler.WebClientInterceptor;
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
 * Inclut propagation JWT et timeouts.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${AUTH_SERVER_ISSUER_URI:http://localhost:9001}")
    private String authorizationServerUrl;

    @Value("${services.authorization-server.timeout:5000}")
    private int timeout;

    /**
     * WebClient pour communiquer avec Authorization Server.
     */
    @Bean("authServerWebClient")
    public WebClient authServerWebClient(WebClient.Builder builder) {
        log.info("Configuring WebClient for Authorization Server: {}", authorizationServerUrl);

        HttpClient httpClient = createHttpClient();

        return builder
                .baseUrl(authorizationServerUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
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
    private HttpClient createHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .responseTimeout(Duration.ofMillis(timeout))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout, TimeUnit.MILLISECONDS)));
    }
}