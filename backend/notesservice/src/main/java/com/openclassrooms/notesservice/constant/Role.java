package com.openclassrooms.notesservice.constant;

/**
 * Constantes de rôles pour les autorisations @PreAuthorize.

 * SUPER_ADMIN a accès à tout.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-02
 */
public class Role {

    private Role() {}

    /**
     * Réservé aux administrateurs (SUPER_ADMIN, ADMIN).
     */
    public static final String ADMIN_ONLY = "hasAnyRole('SUPER_ADMIN', 'ADMIN')";

    /**
     * Réservé aux praticiens (inclut SUPER_ADMIN pour les tests).
     */
    public static final String PRACTITIONER_ONLY = "hasAnyRole('SUPER_ADMIN', 'HEAD_PRACTITIONER', 'PRACTITIONER', 'DOCTOR')";

    /**
     * Réservé aux organisateurs.
     */
    public static final String ORGANIZER_ONLY = "hasAnyRole('SUPER_ADMIN', 'ORGANIZER')";


    /**
     * Tout le personnel médical et administratif.
     */
    public static final String ALL_STAFF = "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HEAD_PRACTITIONER', 'PRACTITIONER', 'DOCTOR', 'ORGANIZER')";

    /**
     * Administrateurs ou praticiens.
     */
    public static final String ADMIN_OR_PRACTITIONER = "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HEAD_PRACTITIONER', 'PRACTITIONER', 'DOCTOR')";

    /**
     * Tout utilisateur authentifié.
     */
    public static final String ALL_AUTHENTICATED = "isAuthenticated()";
}