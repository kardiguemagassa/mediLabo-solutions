package com.openclassrooms.notificationservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

/**
 * DTO contenant les données pour les notifications MediLabo.
 * Utilisé pour transporter les informations via Kafka.
 *
 * @author Kardigué MAGASSA
 * @version 2.1
 * @since 2026-02-09
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Data {

    private String name;
    private String email;
    private String token;

    private String appointmentDate;
    private String appointmentTime;
    private String doctorName;
    private String department;
    private String location;

    private String noteUuid;
    private String patientNumber;
    private String notePreview;
    private String comment;

    private String files;
    private String uploaderName;

    private String analysisType;
    private String sampleDate;
    private String laboratory;

    private String patientUuid;
    private String riskLevel;
    private String riskLevelDescription;
    private String assessmentDate;
    private String assessedAt;
    private Integer triggerCount;
    private Integer patientAge;
    private Integer age;
    private String gender;
    private List<String> triggers;
    private List<String> triggersFound;
    private String recommendation;

    private String senderName;
    private String senderRole;
    private String subject;
    private String messagePreview;

    private String date;
    private String url;
    private String recordNumber;
}