package com.openclassrooms.patientservice.service;

import com.openclassrooms.patientservice.domain.Response;
import com.openclassrooms.patientservice.dtorequest.UserRequest;
import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.service.implementation.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
public class UserServiceImplTest {

    @Mock
    private WebClient authServerWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private UserServiceImpl userService;

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

        // Configuration des mocks
        when(authServerWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // Configuration par défaut - retourne responseSpec pour onStatus
        // Cela simulera que les erreurs ne sont PAS gérées par onStatus
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }

    // TESTS RÉUSSIS

    @Nested
    @DisplayName("getUserByUuid() - Success Cases")
    class GetUserByUuidSuccessTests {

        @Test
        @DisplayName("Should return user when found by UUID")
        void getUserByUuid_userExists_returnsUser() {
            // Given - onStatus déjà configuré dans setUp()
            when(responseSpec.bodyToMono(Response.class)).thenReturn(Mono.just(successResponse));

            // When
            UserRequest result = userService.getUserByUuid("user-uuid-123");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserUuid()).isEqualTo("user-uuid-123");
            assertThat(result.getEmail()).isEqualTo("john.doe@email.com");

            verify(authServerWebClient).get();
            verify(requestHeadersUriSpec).uri("/api/users/{userUuid}", "user-uuid-123");
        }
    }

    @Nested
    @DisplayName("getUserByEmail() - Success Cases")
    class GetUserByEmailSuccessTests {

        @Test
        @DisplayName("Should return user when found by email")
        void getUserByEmail_userExists_returnsOptionalUser() {
            // Given
            Response emailSuccessResponse = new Response(
                    currentTime,
                    HttpStatus.OK.value(),
                    "/api/users/email/john.doe@email.com",
                    HttpStatus.OK,
                    "User retrieved successfully",
                    null,
                    Map.of("user", testUser)
            );

            when(responseSpec.bodyToMono(Response.class)).thenReturn(Mono.just(emailSuccessResponse));

            // When
            Optional<UserRequest> result = userService.getUserByEmail("john.doe@email.com");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("john.doe@email.com");
            assertThat(result.get().getFirstName()).isEqualTo("John");

            verify(requestHeadersUriSpec).uri("/api/users/email/{email}", "john.doe@email.com");
        }
    }

    // TESTS D'ERREUR

    @Nested
    @DisplayName("getUserByUuid() - Error Cases")
    class GetUserByUuidErrorTests {

        @Test
        @DisplayName("Should throw exception when user not found")
        void getUserByUuid_userNotFound_throwsApiException() {
            // Given
            // Simule une WebClientResponseException.NotFound
            WebClientResponseException notFoundException = WebClientResponseException.create(
                    HttpStatus.NOT_FOUND.value(),
                    "Not Found",
                    null, null, null
            );

            // Configuration simplifiée sans thenAnswer complexe
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(notFoundException));

            // When & Then
            assertThatThrownBy(() -> userService.getUserByUuid("user-uuid-999"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Utilisateur non trouvé");
        }
    }

    @Nested
    @DisplayName("getUserByEmail() - Error Cases")
    class GetUserByEmailErrorTests {

        @Test
        @DisplayName("Should return empty optional when user not found by email")
        void getUserByEmail_userNotFound_returnsEmptyOptional() {
            // Given
            // Simule une WebClientResponseException.NotFound pour getUserByEmail
            WebClientResponseException notFoundException = WebClientResponseException.create(
                    HttpStatus.NOT_FOUND.value(),
                    "Not Found",
                    null, null, null
            );

            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(notFoundException));

            // When
            Optional<UserRequest> result = userService.getUserByEmail("unknown@email.com");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty optional when response has no user data")
        void getUserByEmail_responseWithoutUserData_returnsEmptyOptional() {
            // Given
            Response responseNoUserData = new Response(
                    currentTime,
                    HttpStatus.OK.value(),
                    "/api/users/email/john.doe@email.com",
                    HttpStatus.OK,
                    "Success",
                    null,
                    Map.of("other", "data")
            );

            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(Response.class)).thenReturn(Mono.just(responseNoUserData));

            // When
            Optional<UserRequest> result = userService.getUserByEmail("john.doe@email.com");

            // Then
            assertThat(result).isEmpty();
        }
    }

    // TESTS NULL

    @Test
    @DisplayName("Should handle null response gracefully")
    void getUserByUuid_nullResponse_throwsApiException() {
        // Given
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Response.class)).thenReturn(Mono.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserByUuid("user-uuid-123"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Réponse vide");
    }


    // TEST POUR L'API getAssignee

    @Test
    @DisplayName("Should return assignee when found for patient")
    void getAssignee_assigneeExists_returnsUser() {
        // Given
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
                "/api/users/assignee/patient-uuid-123",
                HttpStatus.OK,
                "Assignee retrieved successfully",
                null,
                Map.of("user", assignee)
        );

        when(responseSpec.bodyToMono(Response.class)).thenReturn(Mono.just(assigneeResponse));

        // When
        UserRequest result = userService.getAssignee("patient-uuid-123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserUuid()).isEqualTo("doctor-uuid-456");
        assertThat(result.getRole()).isEqualTo("DOCTOR");

        verify(requestHeadersUriSpec).uri("/api/users/assignee/{patientUuid}", "patient-uuid-123");
    }

    @Test
    @DisplayName("Should throw exception when assignee not found")
    void getAssignee_assigneeNotFound_throwsApiException() {
        // Given
        WebClientResponseException notFoundException = WebClientResponseException.create(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                null, null, null
        );

        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Response.class))
                .thenReturn(Mono.error(notFoundException));

        // When & Then
        assertThatThrownBy(() -> userService.getAssignee("patient-uuid-999"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Assigné non trouvé");
    }

    // exception test

    @Nested
    @DisplayName("getUserByUuid() - Edge Cases")
    class GetUserByUuidEdgeTests {

        @Test
        @DisplayName("Should handle general exception in try-catch")
        void getUserByUuid_generalException_throwsApiException() {
            // Given - Exception générique (pas WebClientResponseException)
            RuntimeException generalException = new RuntimeException("Connection refused");

            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(generalException));

            // When & Then
            assertThatThrownBy(() -> userService.getUserByUuid("user-uuid-123"))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Erreur lors de la communication avec le service utilisateur");
        }

        @Test
        @DisplayName("Should handle ApiException rethrow")
        void getUserByUuid_ApiException_preservesOriginal() {
            // Given - ApiException lancée dans onStatus
            ApiException originalException = new ApiException("Original error message");

            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(originalException));

            // When & Then
            assertThatThrownBy(() -> userService.getUserByUuid("user-uuid-123"))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Original error message");
        }
    }

    @Nested
    @DisplayName("getUserByEmail() - Edge Cases")
    class GetUserByEmailEdgeTests {

        @Test
        @DisplayName("Should handle other 4xx errors (not 404)")
        void getUserByEmail_other4xxError_throwsApiException() {
            // Given - 400 Bad Request (pas 404)
            WebClientResponseException badRequestException = WebClientResponseException.create(
                    HttpStatus.BAD_REQUEST.value(),
                    "Bad Request",
                    null, null, null
            );

            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(badRequestException));

            // When & Then
            assertThatThrownBy(() -> userService.getUserByEmail("john.doe@email.com"))
                    .isInstanceOf(ApiException.class);
            // Le message sera celui du catch générique car ce n'est pas une NotFound
        }



        @Test
        @DisplayName("Should handle ApiException rethrow")
        void getUserByEmail_ApiException_preservesOriginal() {
            // Given - ApiException lancée dans onStatus
            ApiException originalException = new ApiException("Custom API error");

            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(originalException));

            // When & Then
            assertThatThrownBy(() -> userService.getUserByEmail("john.doe@email.com"))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Custom API error");
        }

        @Test
        @DisplayName("Should handle null response")
        void getAssignee_nullResponse_throwsApiException() {
            // Given
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(Response.class)).thenReturn(Mono.empty());

            // When & Then
            assertThatThrownBy(() -> userService.getAssignee("patient-uuid-123"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Réponse vide");
        }

        @Test
        @DisplayName("Should handle general exception")
        void getAssignee_generalException_throwsApiException() {
            // Given
            RuntimeException generalException = new RuntimeException("Network error");

            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(Response.class))
                    .thenReturn(Mono.error(generalException));

            // When & Then
            assertThatThrownBy(() -> userService.getAssignee("patient-uuid-123"))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Erreur lors de la communication avec le service utilisateur");
        }

    }

}