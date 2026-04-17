package com.openclassrooms.notificationservice.utils;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Utilitaires pour les notifications MediLabo.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-09
 */
public class NotificationUtils {

    private NotificationUtils() {}

    public static Supplier<String> randomUUID = () -> UUID.randomUUID().toString();

    /** URLS D'AUTHENTIFICATION */
    public static String getVerificationUrl(String host, String token) {
        return host + "/verify/account?token=" + token;
    }

    public static String getResetPasswordUrl(String host, String token) {
        return host + "/verify/password?token=" + token;
    }

    /**URLS MÉDICALES*/
    public static String getPatientDashboardUrl(String host) {
        return host + "/dashboard";
    }

    public static String getAppointmentUrl(String host, String appointmentId) {
        return host + "/appointments/" + appointmentId;
    }

    public static String getNoteUrl(String host, String noteUuid) {
        return host + "/notes/" + noteUuid;
    }

    public static String getResultsUrl(String host) {
        return host + "/results";
    }

    public static String getMessagesUrl(String host) {
        return host + "/messages";
    }

    public static String getConversationUrl(String host, String conversationId) {
        return host + "/messages/" + conversationId;
    }

    public static String getRiskAssessmentUrl(String host) {
        return host + "/risk-assessment";
    }
}