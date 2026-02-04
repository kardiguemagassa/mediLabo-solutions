package com.openclassrooms.assessmentservice.dtoresponse;

import com.openclassrooms.assessmentservice.model.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO représentant la réponse du Patient Service.
 * Contient uniquement les champs nécessaires pour l'évaluation du risque.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponse {
    private String patientUuid;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;

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
        return firstName + " " + lastName;
    }
}