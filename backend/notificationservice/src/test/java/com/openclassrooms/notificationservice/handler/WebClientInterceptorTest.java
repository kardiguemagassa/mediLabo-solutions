package com.openclassrooms.notificationservice.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebClientInterceptorTest {

    @Mock
    private Jwt jwt;

    @Mock
    private ExchangeFunction exchangeFunction;

    @Mock
    private ClientResponse clientResponse;

    private ClientRequest clientRequest;

    @BeforeEach
    void setUp() {
        URI testUri = URI.create("http://test.com/api");
        clientRequest = ClientRequest.create(HttpMethod.GET, testUri)
                .build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logResponse_ShouldLogResponseStatus() {
        // Given
        HttpStatusCode statusCode = HttpStatus.OK;
        when(clientResponse.statusCode()).thenReturn(statusCode);

        var filter = WebClientInterceptor.logResponse();

        // When
        var result = filter.filter(clientRequest, (req) -> Mono.just(clientResponse));

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(statusCode, response.statusCode());
                })
                .verifyComplete();
    }

    @Test
    void logResponse_ShouldLogErrorResponse() {
        // Given
        HttpStatusCode statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        when(clientResponse.statusCode()).thenReturn(statusCode);

        var filter = WebClientInterceptor.logResponse();

        // When
        var result = filter.filter(clientRequest, (req) -> Mono.just(clientResponse));

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(statusCode, response.statusCode());
                })
                .verifyComplete();
    }

    @Test
    void logResponse_ShouldLogMultipleResponses() {
        // Given
        HttpStatusCode[] statusCodes = {
                HttpStatus.OK,
                HttpStatus.NOT_FOUND,
                HttpStatus.INTERNAL_SERVER_ERROR
        };

        var filter = WebClientInterceptor.logResponse();

        for (HttpStatusCode statusCode : statusCodes) {
            // When
            when(clientResponse.statusCode()).thenReturn(statusCode);

            var result = filter.filter(clientRequest, (req) -> Mono.just(clientResponse));

            // Then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertEquals(statusCode, response.statusCode());
                    })
                    .verifyComplete();
        }
    }

    @Test
    void logResponse_ShouldHandleAllHttpStatusCategories() {
        // Given
        HttpStatusCode[] statusCodes = {
                HttpStatus.CONTINUE,
                HttpStatus.OK,
                HttpStatus.MULTIPLE_CHOICES,
                HttpStatus.BAD_REQUEST,
                HttpStatus.INTERNAL_SERVER_ERROR
        };

        var filter = WebClientInterceptor.logResponse();

        for (HttpStatusCode statusCode : statusCodes) {
            // When
            when(clientResponse.statusCode()).thenReturn(statusCode);

            var result = filter.filter(clientRequest, (req) -> Mono.just(clientResponse));

            // Then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertEquals(statusCode, response.statusCode());
                    })
                    .verifyComplete();
        }
    }

    @Test
    void handleError_ShouldReturnErrorResponse_WhenErrorStatus() {
        // Given
        HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;
        when(clientResponse.statusCode()).thenReturn(statusCode);

        var filter = WebClientInterceptor.handleError();

        // When
        var result = filter.filter(clientRequest, (req) -> Mono.just(clientResponse));

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(statusCode, response.statusCode());
                })
                .verifyComplete();
    }

    @Test
    void handleError_ShouldReturnResponse_WhenClientErrorStatus() {
        // Given
        HttpStatusCode[] clientErrors = {
                HttpStatus.BAD_REQUEST,
                HttpStatus.UNAUTHORIZED,
                HttpStatus.FORBIDDEN,
                HttpStatus.NOT_FOUND,
                HttpStatus.METHOD_NOT_ALLOWED,
                HttpStatus.CONFLICT,
                HttpStatus.UNPROCESSABLE_ENTITY,
                HttpStatus.TOO_MANY_REQUESTS
        };

        var filter = WebClientInterceptor.handleError();

        for (HttpStatusCode status : clientErrors) {
            // When
            when(clientResponse.statusCode()).thenReturn(status);

            var result = filter.filter(clientRequest, (req) -> Mono.just(clientResponse));

            // Then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertEquals(status, response.statusCode());
                        assertTrue(response.statusCode().is4xxClientError());
                    })
                    .verifyComplete();
        }
    }

    @Test
    void handleError_ShouldReturnResponse_WhenServerErrorStatus() {
        // Given
        HttpStatusCode[] serverErrors = {
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.NOT_IMPLEMENTED,
                HttpStatus.BAD_GATEWAY,
                HttpStatus.SERVICE_UNAVAILABLE,
                HttpStatus.GATEWAY_TIMEOUT,
                HttpStatus.HTTP_VERSION_NOT_SUPPORTED
        };

        var filter = WebClientInterceptor.handleError();

        for (HttpStatusCode status : serverErrors) {
            // When
            when(clientResponse.statusCode()).thenReturn(status);

            var result = filter.filter(clientRequest, (req) -> Mono.just(clientResponse));

            // Then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertEquals(status, response.statusCode());
                        assertTrue(response.statusCode().is5xxServerError());
                    })
                    .verifyComplete();
        }
    }

    @Test
    void handleError_ShouldReturnSuccessResponse_WhenNoError() {
        // Given
        HttpStatusCode statusCode = HttpStatus.OK;
        when(clientResponse.statusCode()).thenReturn(statusCode);

        var filter = WebClientInterceptor.handleError();

        // When
        var result = filter.filter(clientRequest, (req) -> Mono.just(clientResponse));

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(statusCode, response.statusCode());
                    assertFalse(response.statusCode().isError());
                })
                .verifyComplete();
    }

    @Test
    void handleError_ShouldReturnSuccessResponse_ForAllSuccessStatuses() {
        // Given
        HttpStatusCode[] successStatuses = {
                HttpStatus.OK,
                HttpStatus.CREATED,
                HttpStatus.ACCEPTED,
                HttpStatus.NO_CONTENT,
                HttpStatus.RESET_CONTENT,
                HttpStatus.PARTIAL_CONTENT,
                HttpStatus.MULTI_STATUS,
                HttpStatus.ALREADY_REPORTED,
                HttpStatus.IM_USED
        };

        var filter = WebClientInterceptor.handleError();

        for (HttpStatusCode status : successStatuses) {
            // When
            when(clientResponse.statusCode()).thenReturn(status);

            var result = filter.filter(clientRequest, (req) -> Mono.just(clientResponse));

            // Then
            StepVerifier.create(result)
                    .assertNext(response -> {
                        assertEquals(status, response.statusCode());
                        assertFalse(response.statusCode().isError());
                    })
                    .verifyComplete();
        }
    }

    @Test
    void allFilters_ShouldWorkTogether() {
        // Given
        String tokenValue = "test.jwt.token";
        when(jwt.getTokenValue()).thenReturn(tokenValue);

        HttpStatusCode statusCode = HttpStatus.OK;
        when(clientResponse.statusCode()).thenReturn(statusCode);

        var authentication = new UsernamePasswordAuthenticationToken(jwt, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When
        var requestResult = WebClientInterceptor.jwtAuthorizationFilter()
                .andThen(WebClientInterceptor.logRequest())
                .filter(clientRequest, (req) ->
                        WebClientInterceptor.logResponse()
                                .andThen(WebClientInterceptor.handleError())
                                .filter(req, r -> Mono.just(clientResponse))
                );

        // Then
        StepVerifier.create(requestResult)
                .assertNext(response -> {
                    assertEquals(statusCode, response.statusCode());
                })
                .verifyComplete();
    }
}