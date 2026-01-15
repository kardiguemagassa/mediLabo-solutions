package com.openclassrooms.patientservice.client;

import com.openclassrooms.patientservice.dto.UserInfoDTO;
import com.openclassrooms.patientservice.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client pour communiquer avec Authorization Server
 * Récupère les informations utilisateur
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-01-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServerClient {

    private final RestTemplate restTemplate;

    @Value("${auth.server.service-name}")
    private String authServerServiceName;

    @Value("${auth.server.url}")
    private String authServerUrl;

    @Value("${auth.server.use-eureka}")
    private boolean useEureka;

    /**
     * Récupérer les informations utilisateur depuis Authorization Server
     */
    public UserInfoDTO getUserInfo(String userUuid) {
        try {
            String url = buildUrl("/api/users/" + userUuid);

            log.debug("Fetching user info from: {}", url);

            UserInfoDTO userInfo = restTemplate.getForObject(url, UserInfoDTO.class);

            if (userInfo == null) {
                throw new ApiException("User not found: " + userUuid);
            }

            return userInfo;

        } catch (Exception e) {
            log.error("Error fetching user info for UUID {}: {}", userUuid, e.getMessage());
            throw new ApiException("Failed to fetch user information: " + e.getMessage());
        }
    }

    /**
     * Vérifier si un utilisateur existe
     */
    public boolean userExists(String userUuid) {
        try {
            String url = buildUrl("/api/users/" + userUuid + "/exists");

            log.debug("Checking if user exists: {}", url);

            Boolean exists = restTemplate.getForObject(url, Boolean.class);

            return Boolean.TRUE.equals(exists);

        } catch (Exception e) {
            log.error("Error checking user existence for UUID {}: {}", userUuid, e.getMessage());
            return false;
        }
    }

    /**
     * Vérifier si Authorization Server est disponible
     */
    public boolean isAuthServerAvailable() {
        try {
            String url = buildUrl("/actuator/health");

            restTemplate.getForObject(url, String.class);

            return true;

        } catch (Exception e) {
            log.warn("Authorization Server unavailable: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Construire l'URL selon la configuration (Eureka ou direct)
     */
    private String buildUrl(String path) {
        if (useEureka) {
            // Utiliser le service name pour Eureka
            return "http://" + authServerServiceName + path;
        } else {
            // Utiliser l'URL directe
            return authServerUrl + path;
        }
    }
}