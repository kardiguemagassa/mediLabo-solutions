package com.openclassrooms.patientservice.service;

import com.openclassrooms.patientservice.dtorequest.UserRequest;
import reactor.core.publisher.Mono;

/**
 * Service pour la communication avec Authorization Server.
 * Récupère les informations utilisateur via API REST.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-01-09
 */
public interface UserServiceClient {

    Mono<UserRequest> getUserByUuid(String userUuid);
    Mono<UserRequest> getUserByEmail(String email);
    Mono<UserRequest> getAssignee(String patientUuid);
}