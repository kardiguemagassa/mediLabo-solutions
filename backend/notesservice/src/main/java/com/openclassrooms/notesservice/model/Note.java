package com.openclassrooms.notesservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Entité Note - Document MongoDB
 * Représente une note d'observation médicale
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 *  @since 2026-02-02
 */

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notes")
@CompoundIndex(name = "idx_patient_active_created", def = "{'patientUuid': 1, 'active': 1, 'createdAt': -1}")
public class Note {

    @Id
    private String id;
    @Indexed(unique = true)
    private String noteUuid;
    @Indexed
    private String patientUuid;
    @Indexed
    private String practitionerUuid;
    private String practitionerName;  // Pour affichage (évite un appel API)
    private String content;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}