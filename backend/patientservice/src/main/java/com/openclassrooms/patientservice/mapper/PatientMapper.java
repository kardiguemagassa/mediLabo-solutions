package com.openclassrooms.patientservice.mapper;

import com.openclassrooms.patientservice.dto.PatientRequestDTO;
import com.openclassrooms.patientservice.dto.PatientResponseDTO;
import com.openclassrooms.patientservice.dto.UserRequestDTO;
import com.openclassrooms.patientservice.model.Patient;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

/**
 * Mapper MapStruct pour les conversions Patient Entity ↔ DTOs.
 * L'implémentation est généré automatiquement à la compilation.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-04-02
 */

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, imports = UUID.class)
public interface PatientMapper {

    /**
     * Convertit une requête de création en entité Patient
     * @param request DTO de création
     * @param medicalRecordNumber numéro de dossier généré
     * @return entité Patient
     */
    @Mapping(target = "patientId", ignore = true)
    @Mapping(target = "patientUuid", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "medicalRecordNumber", source = "medicalRecordNumber")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Patient toEntity(PatientRequestDTO request, String medicalRecordNumber);

    /**
     * Met à jour une entité existante avec les données de la requête
     * @param existing entité existante
     * @param request nouvelles données
     * @return entité mise à jour
     */
    @Mapping(target = "patientId", ignore = true)
    @Mapping(target = "patientUuid", ignore = true)
    @Mapping(target = "userUuid", ignore = true)
    @Mapping(target = "medicalRecordNumber", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Patient updateEntity(@MappingTarget Patient existing, PatientRequestDTO request);

    /**
     * Convertit une entité Patient en DTO de réponse
     * @param patient entité
     * @return DTO de réponse
     */
    @Mapping(target = "age", expression = "java(patient.getAge())")
    @Mapping(target = "bmi", expression = "java(patient.getBMI())")
    @Mapping(target = "userInfo", ignore = true)
    PatientResponseDTO toResponse(Patient patient);

    /**
     * Convertit une liste d'entités en liste de DTOs
     * @param patients liste d'entités
     * @return liste de DTOs
     */
    List<PatientResponseDTO> toResponseList(List<Patient> patients);

    /**
     * ENRICH : Patient + UserRequest → PatientResponse avec UserInfo
     */
    default PatientResponseDTO toResponseWithUserInfo(Patient patient, UserRequestDTO user) {
        PatientResponseDTO response = toResponse(patient);
        if (user != null) {
            response.setUserInfo(toUserInfo(user));
        }
        return response;
    }

    /**
     * Convertit une entité Patient en DTO avec informations utilisateur
     * @param user données utilisateur depuis Auth Server
     * @return DTO de réponse enrichi
     */
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "imageUrl", source = "imageUrl")
    PatientResponseDTO.UserInfo toUserInfo(UserRequestDTO user);
}