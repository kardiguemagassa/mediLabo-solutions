package com.openclassrooms.patientservice.handler;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import static org.apache.http.HttpHeaders.AUTHORIZATION;

public class WebClientInterceptor {


    public static ExchangeFilterFunction authorizationHeaderFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {

            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String token = jwtAuth.getToken().getTokenValue();

                ClientRequest newRequest = ClientRequest.from(request)
                        .header(AUTHORIZATION, "Bearer " + token)
                        .build();

                return Mono.just(newRequest);
            }

            return Mono.just(request);
        });
    }
}
