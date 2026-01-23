package com.openclassrooms.patientservice.handler;

import com.openclassrooms.patientservice.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

import static org.apache.http.HttpHeaders.AUTHORIZATION;

public class RestClientInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(@NonNull HttpRequest request, byte @NonNull [] body, @NonNull ClientHttpRequestExecution execution) throws IOException {

        try {
            HttpServletRequest httpServletRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            var token = httpServletRequest.getHeader(AUTHORIZATION);
            request.getHeaders().add(AUTHORIZATION, token == null ? "" : token);
            return execution.execute(request, body);
        } catch (Exception exception) {
            throw new ApiException("Impossible d'exécuter la requête");
        }

    }
}
