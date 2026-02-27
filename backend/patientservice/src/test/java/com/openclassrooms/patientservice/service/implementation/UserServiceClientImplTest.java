package com.openclassrooms.patientservice.service.implementation;

import com.openclassrooms.patientservice.domain.Response;
import com.openclassrooms.patientservice.dtorequest.UserRequest;
import com.openclassrooms.patientservice.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceClientImpl Unit Tests")
public class UserServiceClientImplTest {

    @Mock
    private WebClient authServerWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private UserServiceClientImpl userService;

    private UserRequest testUser;
    private Response successResponse;
    private String currentTime;

    @BeforeEach
    void setUp() {
        currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        testUser = UserRequest.builder()
                .userUuid("user-uuid-123")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@email.com")
                .phone("+1234567890")
                .address("123 Main St")
                .imageUrl("http://example.com/image.jpg")
                .memberId("M123456")
                .role("PATIENT")
                .enabled(true)
                .accountNonLocked(true)
                .build();

        successResponse = new Response(
                currentTime,
                HttpStatus.OK.value(),
                "/api/users/user-uuid-123",
                HttpStatus.OK,
                "User retrieved successfully",
                null,
                Map.of("user", testUser)
        );
    }

    // Helper pour configurer les mocks

    private void setupWebClientMocks() {
        when(authServerWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }

    //  getUserByUuid - Success

    @Nested
    @DisplayName("getUserByUuid() - Success Cases")
    class GetUserByUuidSuccessTests {

        @Test
        @DisplayName("Should return user when found by UUID")
        void getUserByUuid_userExists_returnsUser() {
            // Given
            setupWebClientMocks();
            when(responseSpec.bodyToMono(Response.class)).thenReturn(Mono.just(successResponse));

            // When & Then
            StepVerifier.create(userService.getUserByUuid("user-uuid-123"))
                    .assertNext(result -> {
                        assertThat(result).isNotNull();
                        assertThat(result.getUserUuid()).isEqualTo("user-uuid-123");
                        assertThat(result.getEmail()).isEqualTo("john.doe@email.com");
                    })
                    .verifyComplete();

            verify(requestHeadersUriSpec).uri("/user/{userUuid}", "user-uuid-123");
        }
    }

    // getUserByUuid - Error Cases

    @Nested
    @DisplayName("getUserByUuid() - Error Cases")
    class GetUserByUuidErrorTests {

        @Test
        @DisplayName("Should throw ApiException when user not found")
        void getUserByUuid_userNotFound_throwsApiException() {
            // Given
            setupWebClientMocks();

            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(new ApiException("Utilisateur non trouvé: user-uuid-999")));

            // When & Then
            StepVerifier.create(userService.getUserByUuid("user-uuid-999"))
                    .expectErrorMatches(throwable ->
                            throwable instanceof ApiException &&
                                    throwable.getMessage().contains("Utilisateur non trouvé"))
                    .verify();
        }


        @Test
        @DisplayName("Should throw exception on empty response")
        void getUserByUuid_emptyResponse_throwsApiException() {
            // Given
            setupWebClientMocks();
            when(responseSpec.bodyToMono(Response.class)).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(userService.getUserByUuid("user-uuid-123"))
                    .expectErrorMatches(throwable ->
                            throwable instanceof ApiException &&
                                    throwable.getMessage().contains("Réponse vide"))
                    .verify();
        }

        @Test
        @DisplayName("Should throw exception on general error")
        void getUserByUuid_generalException_throwsApiException() {
            // Given
            setupWebClientMocks();
            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(new RuntimeException("Connection refused")));

            // When & Then
            StepVerifier.create(userService.getUserByUuid("user-uuid-123"))
                    .expectError(RuntimeException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should preserve ApiException")
        void getUserByUuid_ApiException_preservesOriginal() {
            // Given
            setupWebClientMocks();
            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(new ApiException("Original error message")));

            // When & Then
            StepVerifier.create(userService.getUserByUuid("user-uuid-123"))
                    .expectErrorMatches(throwable ->
                            throwable instanceof ApiException &&
                                    throwable.getMessage().equals("Original error message"))
                    .verify();
        }
    }

    // getUserByEmail - Success

    @Nested
    @DisplayName("getUserByEmail() - Success Cases")
    class GetUserByEmailSuccessTests {

        @Test
        @DisplayName("Should return user when found by email")
        void getUserByEmail_userExists_returnsUser() {
            // Given
            setupWebClientMocks();
            Response emailSuccessResponse = new Response(
                    currentTime,
                    HttpStatus.OK.value(),
                    "/user/user/john.doe@email.com",
                    HttpStatus.OK,
                    "User retrieved successfully",
                    null,
                    Map.of("user", testUser)
            );
            when(responseSpec.bodyToMono(Response.class)).thenReturn(Mono.just(emailSuccessResponse));

            // When & Then
            StepVerifier.create(userService.getUserByEmail("john.doe@email.com"))
                    .assertNext(result -> {
                        assertThat(result).isNotNull();
                        assertThat(result.getEmail()).isEqualTo("john.doe@email.com");
                        assertThat(result.getFirstName()).isEqualTo("John");
                    })
                    .verifyComplete();


            verify(requestHeadersUriSpec).uri("/user/user/{email}", "john.doe@email.com");
        }
    }

    //  getUserByEmail - Error Cases

    @Nested
    @DisplayName("getUserByEmail() - Error Cases")
    class GetUserByEmailErrorTests {

        @Test
        @DisplayName("Should return empty when user not found by email")
        void getUserByEmail_userNotFound_returnsEmpty() {
            // Given
            setupWebClientMocks();
            // Pour NOT_FOUND, le service retourne Mono.empty() (pas une exception)
            when(responseSpec.bodyToMono(Response.class)).thenReturn(Mono.empty());

            // When & Then - getUserByEmail retourne empty pour NOT_FOUND
            StepVerifier.create(userService.getUserByEmail("unknown@email.com"))
                    .verifyComplete();  // Mono.empty() complète sans émettre
        }

        @Test
        @DisplayName("Should return empty when response has no user data")
        void getUserByEmail_responseWithoutUserData_returnsEmpty() {
            // Given
            setupWebClientMocks();
            Response responseNoUserData = new Response(
                    currentTime,
                    HttpStatus.OK.value(),
                    "/user/user/john.doe@email.com",
                    HttpStatus.OK,
                    "Success",
                    null,
                    Map.of("other", "data")  // Pas de clé "user"
            );
            when(responseSpec.bodyToMono(Response.class)).thenReturn(Mono.just(responseNoUserData));

            // When & Then - filter() filtre cette réponse donc Mono.empty()
            StepVerifier.create(userService.getUserByEmail("john.doe@email.com"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should throw exception on other 4xx errors")
        void getUserByEmail_other4xxError_throwsApiException() {
            // Given
            setupWebClientMocks();
            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(new ApiException("Erreur client lors de la récupération de l'utilisateur")));

            // When & Then
            StepVerifier.create(userService.getUserByEmail("john.doe@email.com"))
                    .expectErrorMatches(throwable ->
                            throwable instanceof ApiException &&
                                    throwable.getMessage().contains("Erreur client"))
                    .verify();
        }

        @Test
        @DisplayName("Should preserve ApiException")
        void getUserByEmail_ApiException_preservesOriginal() {
            // Given
            setupWebClientMocks();
            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(new ApiException("Custom API error")));

            // When & Then
            StepVerifier.create(userService.getUserByEmail("john.doe@email.com"))
                    .expectErrorMatches(throwable ->
                            throwable instanceof ApiException &&
                                    throwable.getMessage().equals("Custom API error"))
                    .verify();
        }
    }

    // getAssignee - Success

    @Nested
    @DisplayName("getAssignee() - Success Cases")
    class GetAssigneeSuccessTests {

        @Test
        @DisplayName("Should return assignee when found for patient")
        void getAssignee_assigneeExists_returnsUser() {
            // Given
            setupWebClientMocks();
            UserRequest assignee = UserRequest.builder()
                    .userUuid("doctor-uuid-456")
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@hospital.com")
                    .role("DOCTOR")
                    .enabled(true)
                    .accountNonLocked(true)
                    .build();

            Response assigneeResponse = new Response(
                    currentTime,
                    HttpStatus.OK.value(),
                    "/user/assignee/patient-uuid-123",
                    HttpStatus.OK,
                    "Assignee retrieved successfully",
                    null,
                    Map.of("user", assignee)
            );
            when(responseSpec.bodyToMono(Response.class)).thenReturn(Mono.just(assigneeResponse));

            // When & Then
            StepVerifier.create(userService.getAssignee("patient-uuid-123"))
                    .assertNext(result -> {
                        assertThat(result).isNotNull();
                        assertThat(result.getUserUuid()).isEqualTo("doctor-uuid-456");
                        assertThat(result.getRole()).isEqualTo("DOCTOR");
                    })
                    .verifyComplete();

            verify(requestHeadersUriSpec).uri("/user/assignee/{patientUuid}", "patient-uuid-123");
        }
    }

    // getAssignee - Error Cases

    @Nested
    @DisplayName("getAssignee() - Error Cases")
    class GetAssigneeErrorTests {

        @Test
        @DisplayName("Should throw ApiException when assignee not found (404)")
        void getAssignee_assigneeNotFound_throwsApiException() {
            // Given
            setupWebClientMocks();
            // Simule le comportement de onStatus pour 404
            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(new ApiException("Assigné non trouvé pour le patient: patient-uuid-999")));

            // When & Then
            StepVerifier.create(userService.getAssignee("patient-uuid-999"))
                    .expectErrorMatches(throwable ->
                            throwable instanceof ApiException &&
                                    throwable.getMessage().contains("Assigné non trouvé"))
                    .verify();
        }

        @Test
        @DisplayName("Should throw ApiException on 4xx client error")
        void getAssignee_4xxClientError_throwsApiException() {
            // Given
            setupWebClientMocks();
            // Simule le comportement de onStatus pour 4xx (autre que 404)
            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(new ApiException("Erreur client lors de la récupération de l'assigné")));

            // When & Then
            StepVerifier.create(userService.getAssignee("patient-uuid-123"))
                    .expectErrorMatches(throwable ->
                            throwable instanceof ApiException &&
                                    throwable.getMessage().contains("Erreur client"))
                    .verify();
        }

        @Test
        @DisplayName("Should throw ApiException on 5xx server error")
        void getAssignee_5xxServerError_throwsApiException() {
            // Given
            setupWebClientMocks();
            // Simule le comportement de onStatus pour 5xx
            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(new ApiException("Erreur serveur Authorization Server")));

            // When & Then
            StepVerifier.create(userService.getAssignee("patient-uuid-123"))
                    .expectErrorMatches(throwable ->
                            throwable instanceof ApiException &&
                                    throwable.getMessage().contains("Erreur serveur"))
                    .verify();
        }

        @Test
        @DisplayName("Should throw ApiException on empty response")
        void getAssignee_emptyResponse_throwsApiException() {
            // Given
            setupWebClientMocks();
            when(responseSpec.bodyToMono(Response.class)).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(userService.getAssignee("patient-uuid-123"))
                    .expectErrorMatches(throwable ->
                            throwable instanceof ApiException &&
                                    throwable.getMessage().contains("Réponse vide"))
                    .verify();
        }

        @Test
        @DisplayName("Should return empty when response has no user data")
        void getAssignee_responseWithoutUserData_throwsApiException() {
            // Given
            setupWebClientMocks();
            Response responseNoUserData = new Response(
                    currentTime,
                    HttpStatus.OK.value(),
                    "/user/assignee/patient-uuid-123",
                    HttpStatus.OK,
                    "Success",
                    null,
                    Map.of("other", "data")  // Pas de clé "user"
            );
            when(responseSpec.bodyToMono(Response.class)).thenReturn(Mono.just(responseNoUserData));

            // When & Then - filter() filtre, puis switchIfEmpty lance l'erreur
            StepVerifier.create(userService.getAssignee("patient-uuid-123"))
                    .expectErrorMatches(throwable ->
                            throwable instanceof ApiException &&
                                    throwable.getMessage().contains("Réponse vide"))
                    .verify();
        }
    }

    // FALLBACK METHODS

    @Nested
    @DisplayName("Fallback Methods Tests")
    class FallbackMethodsTests {

        @Test
        @DisplayName("getUserByUuidFallback should return ApiException")
        void getUserByUuidFallback_returnsApiException() {
            // Given
            Throwable cause = new RuntimeException("Connection timeout");

            // When & Then
            StepVerifier.create(userService.getUserByUuidFallback("user-uuid-123", cause))
                    .expectErrorMatches(throwable ->
                            throwable instanceof ApiException &&
                                    throwable.getMessage().equals("Service utilisateur indisponible"))
                    .verify();
        }

        @Test
        @DisplayName("getUserByEmailFallback should return empty Mono")
        void getUserByEmailFallback_returnsEmpty() {
            // Given
            Throwable cause = new RuntimeException("Connection timeout");

            // When & Then
            StepVerifier.create(userService.getUserByEmailFallback("john@email.com", cause))
                    .verifyComplete();  // Mono.empty() complète sans émettre
        }

        @Test
        @DisplayName("getAssigneeFallback should return ApiException")
        void getAssigneeFallback_returnsApiException() {
            // Given
            Throwable cause = new RuntimeException("Service unavailable");

            // When & Then
            StepVerifier.create(userService.getAssigneeFallback("patient-uuid-123", cause))
                    .expectErrorMatches(throwable ->
                            throwable instanceof ApiException &&
                                    throwable.getMessage().equals("Service utilisateur indisponible"))
                    .verify();
        }
    }



}