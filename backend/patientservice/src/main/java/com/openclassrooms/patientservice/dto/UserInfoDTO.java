package com.openclassrooms.patientservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les informations utilisateur
 * Reçu depuis Authorization Server
 *
 * @author Kardigué MAGASSA
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