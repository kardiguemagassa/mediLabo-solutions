package com.openclassrooms.notesservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les informations patient reçues depuis PatientService.
 * Contient les infos nécessaires pour les notifications.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientInfo {

    private String patientUuid;
    private String userUuid;
    private String medicalRecordNumber;

    /**
     * Informations utilisateur enrichies (depuis Auth Server via PatientService).
     */
    private UserInfo userInfo;

    /**
     * Informations utilisateur.
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo {
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String address;
        private String imageUrl;
    }

    // MÉTHODES UTILITAIRES

    /**
     * Retourne le nom complet du patient.
     */
    public String getFullName() {
        if (userInfo == null) {
            return "Patient " + patientUuid;
        }
        String firstName = userInfo.getFirstName() != null ? userInfo.getFirstName() : "";
        String lastName = userInfo.getLastName() != null ? userInfo.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }

    /**
     * Retourne l'email du patient.
     */
    public String getEmail() {
        return userInfo != null ? userInfo.getEmail() : null;
    }
}