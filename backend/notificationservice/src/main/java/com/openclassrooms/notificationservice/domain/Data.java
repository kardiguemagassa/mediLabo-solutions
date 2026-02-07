package com.openclassrooms.notificationservice.domain;

import lombok.*;

import java.util.List;

/**
 * DTO contenant les données pour les notifications MediLabo.
 * Utilisé pour transporter les informations via Kafka.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Data {

    private String name;
    private String email;
    private String token;
    // RENDEZ-VOUS
    private String appointmentDate;
    private String appointmentTime;
    private String doctorName;
    private String department;
    // Lieu du RDV
    private String location;
    // NOTES MÉDICALES
    private String noteUuid;
    private String patientNumber;
    private String notePreview;
    private String comment;
    // FICHIERS
    private String files;
    private String uploaderName;
    // RÉSULTATS D'ANALYSES
    private String analysisType;
    private String sampleDate;
    private String laboratory;
    // ÉVALUATION RISQUE DIABÈTE
    private String riskLevel;           // Niveau de risque (NONE, BORDERLINE, DANGER, EARLY_ONSET)
    private String assessmentDate;      // Date de l'évaluation
    private Integer triggerCount;       // Nombre de facteurs de risque
    private Integer patientAge;         // Âge du patient
    private List<String> triggers;      // Liste des facteurs de risque détectés
    private String recommendation;      // Recommandation médicale
    // MESSAGES
    private String senderName;
    private String senderRole;
    private String subject;
    private String messagePreview;
    // MÉTADONNÉES
    private String date;
    private String url;
    private String recordNumber;
}