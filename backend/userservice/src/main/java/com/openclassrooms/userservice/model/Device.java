package com.openclassrooms.userservice.model;

import lombok.*;

/**
 * Modèle User pour l'authentification
 * POJO simple mappé depuis SQL (pas d'entité JPA)
 *
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
public class Device {
    private Long deviceId;
    private Long userId;
    private String device;
    private String client;
    private String ipAddress;
    private String createdAt;
    private String updatedAt;
}
