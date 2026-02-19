package com.openclassrooms.patientservice.service.implementation;

import com.openclassrooms.patientservice.domain.Response;
import com.openclassrooms.patientservice.dtorequest.UserRequest;
import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static com.openclassrooms.patientservice.util.RequestUtils.convertResponse;

/**
 * Implémentation du service de communication avec Authorization Server.
 *
 * Utilise WebClient (standard microservices) pour les appels inter-services.
 * Le token JWT est automatiquement propagé via WebClientInterceptor.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-01-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final WebClient authServerWebClient;

    @Override
    public UserRequest getUserByUuid(String userUuid) {
        log.debug("Fetching user by UUID: {}", userUuid);

        try {
            Response response = authServerWebClient.get()
                    .uri("/user/{userUuid}", userUuid)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        if (clientResponse.statusCode() == HttpStatus.NOT_FOUND) {
                            return Mono.error(new ApiException("Utilisateur non trouvé: " + userUuid));
                        }
                        return Mono.error(new ApiException("Erreur lors de la récupération de l'utilisateur"));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            Mono.error(new ApiException("Erreur serveur Authorization Server")))
                    .bodyToMono(Response.class)
                    .block();

            if (response == null || response.data() == null) {
                throw new ApiException("Réponse vide du service utilisateur");
            }

            return convertResponse(response, UserRequest.class, "user");

        } catch (ApiException e) {
            throw e;
        } catch (WebClientResponseException.NotFound e) {
            log.warn("User not found: {}", userUuid);
            throw new ApiException("Utilisateur non trouvé: " + userUuid);
        } catch (Exception e) {
            log.error("Error fetching user by UUID: {}", e.getMessage(), e);
            throw new ApiException("Erreur lors de la communication avec le service utilisateur");
        }
    }

    @Override
    public Optional<UserRequest> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);

        try {
            Response response = authServerWebClient.get()
                    .uri("/user/user/{email}", email)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, clientResponse ->
                            Mono.empty())  // 404 = pas d'erreur, juste Optional.empty()
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            Mono.error(new ApiException("Erreur serveur Authorization Server")))
                    .bodyToMono(Response.class)
                    .block();

            if (response == null || response.data() == null || !response.data().containsKey("user")) {
                log.debug("User not found for email: {}", email);
                return Optional.empty();
            }

            UserRequest user = convertResponse(response, UserRequest.class, "user");
            return Optional.of(user);

        } catch (WebClientResponseException.NotFound e) {
            log.debug("User not found for email: {}", email);
            return Optional.empty();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching user by email: {}", e.getMessage(), e);
            throw new ApiException("Erreur lors de la communication avec le service utilisateur");
        }
    }

    @Override
    public UserRequest getAssignee(String patientUuid) {
        log.debug("Fetching assignee for patient: {}", patientUuid);

        try {
            Response response = authServerWebClient.get()
                    .uri("/user/assignee/{patientUuid}", patientUuid)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        if (clientResponse.statusCode() == HttpStatus.NOT_FOUND) {
                            return Mono.error(new ApiException("Assigné non trouvé pour le patient: " + patientUuid));
                        }
                        return Mono.error(new ApiException("Erreur lors de la récupération de l'assigné"));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            Mono.error(new ApiException("Erreur serveur Authorization Server")))
                    .bodyToMono(Response.class)
                    .block();

            if (response == null || response.data() == null) {
                throw new ApiException("Réponse vide du service utilisateur");
            }

            return convertResponse(response, UserRequest.class, "user");

        } catch (ApiException e) {
            throw e;
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Assignee not found for patient: {}", patientUuid);
            throw new ApiException("Assigné non trouvé pour le patient: " + patientUuid);
        } catch (Exception e) {
            log.error("Error fetching assignee: {}", e.getMessage(), e);
            throw new ApiException("Erreur lors de la communication avec le service utilisateur");
        }
    }
}