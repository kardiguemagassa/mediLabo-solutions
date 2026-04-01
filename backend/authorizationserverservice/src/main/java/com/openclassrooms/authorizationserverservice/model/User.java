package com.openclassrooms.authorizationserverservice.model;

import lombok.*;

/**
 * Modèle User pour l'authentification
 * POJO simple mappé depuis SQL (pas d'entité JPA)
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long userId;
    private String userUuid;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private String bio;
    private String imageUrl;
    private String memberId;
    private String address;
    private String qrCodeImageUri;
    private String qrCodeSecret;
    private String lastLogin;
    private int loginAttempts;
    private String createdAt;
    private String updatedAt;
    private String role;
    private String authorities;
    private boolean mfa;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;
}