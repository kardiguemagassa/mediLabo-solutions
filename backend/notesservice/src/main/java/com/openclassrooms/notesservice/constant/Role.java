package com.openclassrooms.notesservice.constant;

/**
 *  @author Kardigué MAGASSA
 *  @version 1.0
 *  @since 2026-02-02
 */

public class Role {

    private Role() {}

    // Rôles individuels
    public static final String ADMIN_ONLY = "hasAnyRole('SUPER_ADMIN','ADMIN')";
    public static final String PRACTITIONER_ONLY = "hasAnyRole('PRACTITIONER', 'DOCTOR')";
    public static final String ORGANIZER_ONLY = "hasRole('ORGANIZER')";

    // Combinaisons
    public static final String ALL_STAFF = "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRACTITIONER', 'DOCTOR', 'ORGANIZER')";
    public static final String ADMIN_OR_PRACTITIONER = "hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRACTITIONER', 'DOCTOR')";
    public static final String ALL_AUTHENTICATED = "isAuthenticated()";
}