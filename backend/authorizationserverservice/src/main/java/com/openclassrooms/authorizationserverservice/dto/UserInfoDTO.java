package com.openclassrooms.authorizationserverservice.dto;

import lombok.*;

/**
 * DTO pour exposer les informations utilisateur
 * Utilisé par les autres microservices (Patient Service, etc.)
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {

    private String userUuid;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String imageUrl;
    private String memberId;
    private String role;
    private Boolean enabled;
    private Boolean accountNonLocked;
}