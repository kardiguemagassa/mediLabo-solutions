package com.openclassrooms.patientservice.util;

import com.openclassrooms.patientservice.constant.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

/**
 * Utilitaire pour la gestion des permissions utilisateur.
 *
 * Vérifie les rôles depuis le SecurityContext.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-01-09
 */
@Slf4j
public final class UserUtils {

    private UserUtils() {}

    // Rôles avec permissions élevées
    private static final Set<String> ELEVATED_ROLES = Set.of(Role.SUPER_ADMIN, Role.ADMIN, Role.ORGANIZER, Role.PRACTITIONER);

    /**
     * Vérifie si l'utilisateur courant a des permissions élevées.
     * @return true si ADMIN, PRACTITIONER, ORGANIZER ou SUPER_ADMIN
     */
    public static boolean hasElevatedPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(ELEVATED_ROLES::contains);
    }

    /**
     * Vérifie si l'utilisateur courant a un rôle spécifique.
     * @param role rôle à vérifier
     * @return true si l'utilisateur a ce rôle
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(auth -> auth.equals(role));
    }

    /**
     * Récupère l'UUID de l'utilisateur courant.
     * @return userUuid ou null si non authentifié
     */
    public static String getCurrentUserUuid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        return authentication.getName();
    }

    /**
     * Vérifie si l'utilisateur courant est le propriétaire de la ressource.
     * @param resourceOwnerUuid UUID du propriétaire de la ressource
     * @return true si l'utilisateur courant est le propriétaire
     */
    public static boolean isOwner(String resourceOwnerUuid) {
        String currentUserUuid = getCurrentUserUuid();
        return currentUserUuid != null && currentUserUuid.equals(resourceOwnerUuid);
    }

    /**
     * Vérifie si l'utilisateur peut accéder à une ressource.
     * (propriétaire OU permissions élevées)
     * @param resourceOwnerUuid UUID du propriétaire
     * @return true si accès autorisé
     */
    public static boolean canAccess(String resourceOwnerUuid) {
        return isOwner(resourceOwnerUuid) || hasElevatedPermissions();
    }
}