package com.openclassrooms.assessmentservice.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.assessmentservice.domain.Response;
import com.openclassrooms.assessmentservice.dtoresponse.PatientResponse;
import com.openclassrooms.assessmentservice.exception.ApiException;
import com.openclassrooms.assessmentservice.service.PatientServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientServiceClientImpl implements PatientServiceClient {

    private final WebClient patientServiceWebClient;
    private final ObjectMapper objectMapper;

    private static final String CIRCUIT_BREAKER_NAME = "patientService";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Retry(name = CIRCUIT_BREAKER_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getPatientByUuidFallback")
    public Mono<PatientResponse> getPatientByUuid(String patientUuid, String token) {
        log.info("Fetching patient from PatientService: {}", patientUuid);

        return patientServiceWebClient.get()
                .uri("/api/patients/{patientUuid}", patientUuid)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), response -> Mono.empty())
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new ApiException("Erreur service Patient")))
                .bodyToMono(Response.class)
                .flatMap(response -> {
                    if (response == null || response.data() == null || !response.data().containsKey("patient")) {
                        return Mono.empty();
                    }
                    return Mono.just(extractPatient(response));
                })
                .timeout(TIMEOUT);
    }

    public Mono<PatientResponse> getPatientByUuidFallback(String patientUuid, String token, Throwable throwable) {
        log.error("Fallback getPatientByUuid - UUID: {}, Cause: {}", patientUuid, throwable.getMessage());
        return Mono.empty();
    }

    private PatientResponse extractPatient(Response response) {
        Object patientData = response.data().get("patient");
        if (patientData == null) {
            throw new ApiException("Données patient manquantes");
        }

        return objectMapper.convertValue(patientData, PatientResponse.class);
    }

}