package com.openclassrooms.notificationservice.dtorequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * DTO pour les informations utilisateur
 * Reçu depuis Authorization Server
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRequest {
    private String userUuid;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String imageUrl;
    private String role;
}