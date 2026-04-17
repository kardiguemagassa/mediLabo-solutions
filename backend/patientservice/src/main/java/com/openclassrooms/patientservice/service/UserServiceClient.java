package com.openclassrooms.patientservice.service;

import com.openclassrooms.patientservice.dto.UserRequestDTO;
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

    Mono<UserRequestDTO> getUserByUuid(String userUuid);
    Mono<UserRequestDTO> getUserByEmail(String email);
    Mono<UserRequestDTO> getAssignee(String patientUuid);
    Mono<UserRequestDTO> updateUserContactInfo(String userUuid, String phone, String address);
}