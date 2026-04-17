package com.openclassrooms.patientservice.constant;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
public final class Role {

    private Role() {}

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";
    public static final String PRACTITIONER = "PRACTITIONER";
    public static final String ORGANIZER = "ORGANIZER";
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    // Groupes de permissions pour @PreAuthorize
    public static final String ALL_STAFF = "hasAnyAuthority('SUPER_ADMIN', 'ORGANIZER', 'ADMIN', 'PRACTITIONER')";
    public static final String ADMIN_ONLY = "hasAnyAuthority('SUPER_ADMIN', 'ADMIN')";
    public static final String ALL_AUTHENTICATED = "hasAnyAuthority('SUPER_ADMIN', 'ORGANIZER', 'ADMIN', 'PRACTITIONER', 'USER')";
}