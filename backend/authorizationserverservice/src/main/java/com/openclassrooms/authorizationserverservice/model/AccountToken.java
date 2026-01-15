package com.openclassrooms.patientservice.model;

import lombok.*;

/**
 * Modèle User pour l'authentification
 * POJO simple mappé depuis SQL (pas d'entité JPA)
 * @author FirstName LastName
 * @version 1.0
 * @email magassa***REMOVED_USER***@gmail.com
 * @since 2026-05-01
 */

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountToken {
    private Long accountTokenId;
    private Long userId;
    private String token;
    private boolean expired;
    private String createdAt;
    private String updatedAt;
}