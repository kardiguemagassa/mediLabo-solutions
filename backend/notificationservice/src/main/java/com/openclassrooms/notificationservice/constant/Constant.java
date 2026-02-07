package com.openclassrooms.notificationservice.constant;

/**
 * Constantes pour les notifications MediLabo.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */
public class Constant {

    private Constant() {}

    // ENCODAGE
    public static final String UTF_8_ENCODING = "UTF-8";

    // SUJETS D'EMAIL
    public static final String SUBJECT_ACCOUNT_VERIFICATION = "MediLabo - Vérification de votre compte";
    public static final String SUBJECT_PASSWORD_RESET = "MediLabo - Réinitialisation de votre mot de passe";
    public static final String SUBJECT_APPOINTMENT_CONFIRMATION = "MediLabo - Confirmation de votre rendez-vous";
    public static final String SUBJECT_APPOINTMENT_REMINDER = "MediLabo - Rappel de rendez-vous";
    public static final String SUBJECT_APPOINTMENT_CANCELLED = "MediLabo - Annulation de votre rendez-vous";
    public static final String SUBJECT_NEW_NOTE = "MediLabo - Nouvelle note médicale";
    public static final String SUBJECT_NEW_COMMENT = "MediLabo - Nouveau commentaire sur votre dossier";
    public static final String SUBJECT_NEW_FILES = "MediLabo - Nouveaux documents disponibles";
    public static final String SUBJECT_RESULTS_AVAILABLE = "MediLabo - Vos résultats d'analyses sont disponibles";
    public static final String SUBJECT_RISK_ASSESSMENT = "MediLabo - Évaluation de votre risque diabète";
    public static final String SUBJECT_NEW_MESSAGE = "MediLabo - Nouveau message";

    // NOMS DES TEMPLATES THYMELEAF CORRESPONDRE AUX FICHIERS.THML
    public static final String TEMPLATE_ACCOUNT_VERIFICATION = "account-verification";
    public static final String TEMPLATE_PASSWORD_RESET = "password-reset";
    public static final String TEMPLATE_APPOINTMENT_CONFIRMATION = "appointment-confirmation";
    public static final String TEMPLATE_APPOINTMENT_REMINDER = "appointment-reminder";
    public static final String TEMPLATE_NEW_NOTE = "new-medical-note";
    public static final String TEMPLATE_NEW_COMMENT = "new-comment";
    public static final String TEMPLATE_NEW_FILES = "new-files-uploaded";
    public static final String TEMPLATE_RESULTS_AVAILABLE = "results-available";
    public static final String TEMPLATE_RISK_ASSESSMENT = "risk-assessment";
    public static final String TEMPLATE_NEW_MESSAGE = "new-message";

}