package com.openclassrooms.assessmentservice.dtoresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.openclassrooms.assessmentservice.model.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO représentant la réponse du Patient Service.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientResponseDTO {
    private String patientUuid;
    private LocalDate dateOfBirth;
    private Gender gender;
    private UserInfo userInfo;

    /**
     * Infos utilisateur imbriquées.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo {
        private String firstName;
        private String lastName;
        private String email;
        private String imageUrl;
    }

    /**
     * Calcule l'âge du patient en années.
     */
    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return java.time.Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Retourne le nom complet du patient.
     */
    public String getFullName() {
        if (userInfo == null) {
            return "Inconnu";
        }
        String first = userInfo.getFirstName() != null ? userInfo.getFirstName() : "";
        String last = userInfo.getLastName() != null ? userInfo.getLastName() : "";
        return (first + " " + last).trim();
    }
}