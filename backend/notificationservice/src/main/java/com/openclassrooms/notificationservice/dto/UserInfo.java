package com.openclassrooms.notificationservice.dto;

import lombok.*;

/**
 * DTO pour les informations utilisateur récupérées depuis l'Auth Server.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {
    private String userUuid;
    private String firstName;
    private String lastName;
    private String email;
    private String imageUrl;
    private String role;

    /**
     * Retourne le nom complet (prénom + nom).
     */
    public String getName() {
        return firstName + " " + lastName;
    }
}