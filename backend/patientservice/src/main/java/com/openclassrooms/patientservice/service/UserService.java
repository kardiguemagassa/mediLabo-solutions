package com.openclassrooms.patientservice.service;

import com.openclassrooms.patientservice.dtorequest.UserRequest;

import java.util.Optional;

/**
 * Service pour la communication avec Authorization Server.
 * Récupère les informations utilisateur via API REST.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-01-09
 */
public interface UserService {

    /**
     * Récupère un utilisateur par son UUID
     * @param userUuid UUID de l'utilisateur
     * @return informations utilisateur
     */
    UserRequest getUserByUuid(String userUuid);

    /**
     * Récupère un utilisateur par son email
     * @param email email de l'utilisateur
     * @return Optional contenant les informations utilisateur si trouvé
     */
    Optional<UserRequest> getUserByEmail(String email);

    /**
     * Récupère l'assigné d'un patient
     * @param patientUuid UUID du patient
     * @return informations de l'assigné
     */
    UserRequest getAssignee(String patientUuid);
}