package com.openclassrooms.notificationservice.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class NotificationUtilsTest {

    @Test
    void randomUUID_ShouldGenerateValidUUID() {
        // When
        String uuidString = NotificationUtils.randomUUID.get();

        // Then
        assertNotNull(uuidString);
        assertDoesNotThrow(() -> UUID.fromString(uuidString));

        UUID uuid = UUID.fromString(uuidString);
        assertEquals(36, uuidString.length());
        assertEquals(4, uuid.version());
    }

    @Test
    void randomUUID_ShouldGenerateUniqueUUIDs() {
        // When
        String uuid1 = NotificationUtils.randomUUID.get();
        String uuid2 = NotificationUtils.randomUUID.get();
        String uuid3 = NotificationUtils.randomUUID.get();

        // Then
        assertNotNull(uuid1);
        assertNotNull(uuid2);
        assertNotNull(uuid3);
        assertNotEquals(uuid1, uuid2);
        assertNotEquals(uuid1, uuid3);
        assertNotEquals(uuid2, uuid3);
    }

    @Test
    void randomUUID_ShouldGenerateUUIDWithCorrectFormat() {
        // When
        String uuid = NotificationUtils.randomUUID.get();

        // Then
        assertTrue(uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    // TESTS POUR LES URLS D'AUTHENTIFICATION

    @ParameterizedTest
    @MethodSource("provideHostAndTokenForVerification")
    void getVerificationUrl_ShouldBuildCorrectUrl(String host, String token, String expectedUrl) {
        // When
        String result = NotificationUtils.getVerificationUrl(host, token);

        // Then
        assertEquals(expectedUrl, result);
    }

    private static Stream<Arguments> provideHostAndTokenForVerification() {
        return Stream.of(
                Arguments.of("https://medilabo.fr", "abc123", "https://medilabo.fr/verify/account?token=abc123"),
                Arguments.of("http://localhost:8080", "token-456", "http://localhost:8080/verify/account?token=token-456"),
                Arguments.of("https://api.medilabo.com", "xyz789", "https://api.medilabo.com/verify/account?token=xyz789"),
                Arguments.of("https://medilabo.fr/", "abc123", "https://medilabo.fr//verify/account?token=abc123") // Note: double slash
        );
    }

    @ParameterizedTest
    @MethodSource("provideHostAndTokenForResetPassword")
    void getResetPasswordUrl_ShouldBuildCorrectUrl(String host, String token, String expectedUrl) {
        // When
        String result = NotificationUtils.getResetPasswordUrl(host, token);

        // Then
        assertEquals(expectedUrl, result);
    }

    private static Stream<Arguments> provideHostAndTokenForResetPassword() {
        return Stream.of(
                Arguments.of("https://medilabo.fr", "abc123", "https://medilabo.fr/verify/password?token=abc123"),
                Arguments.of("http://localhost:8080", "token-456", "http://localhost:8080/verify/password?token=token-456"),
                Arguments.of("https://api.medilabo.com", "xyz789", "https://api.medilabo.com/verify/password?token=xyz789")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void getVerificationUrl_ShouldHandleInvalidHost(String invalidHost) {
        // Given
        String token = "abc123";

        // When
        String result = NotificationUtils.getVerificationUrl(invalidHost, token);

        // Then
        assertTrue(result.contains("/verify/account?token=" + token));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void getResetPasswordUrl_ShouldHandleInvalidHost(String invalidHost) {
        // Given
        String token = "abc123";

        // When
        String result = NotificationUtils.getResetPasswordUrl(invalidHost, token);

        // Then
        assertTrue(result.contains("/verify/password?token=" + token));
    }

    @Test
    void getVerificationUrl_ShouldHandleNullToken() {
        // Given
        String host = "https://medilabo.fr";

        // When
        String result = NotificationUtils.getVerificationUrl(host, null);

        // Then
        assertEquals("https://medilabo.fr/verify/account?token=null", result);
    }

    @Test
    void getResetPasswordUrl_ShouldHandleNullToken() {
        // Given
        String host = "https://medilabo.fr";

        // When
        String result = NotificationUtils.getResetPasswordUrl(host, null);

        // Then
        assertEquals("https://medilabo.fr/verify/password?token=null", result);
    }

    //  TESTS POUR LES URLS MÉDICALES

    @ParameterizedTest
    @MethodSource("provideHostForMedicalUrls")
    void getPatientDashboardUrl_ShouldBuildCorrectUrl(String host, String expectedUrl) {
        // When
        String result = NotificationUtils.getPatientDashboardUrl(host);

        // Then
        assertEquals(expectedUrl, result);
    }

    private static Stream<Arguments> provideHostForMedicalUrls() {
        return Stream.of(
                Arguments.of("https://medilabo.fr", "https://medilabo.fr/dashboard"),
                Arguments.of("http://localhost:8080", "http://localhost:8080/dashboard"),
                Arguments.of("https://api.medilabo.com", "https://api.medilabo.com/dashboard"),
                Arguments.of("https://medilabo.fr/", "https://medilabo.fr//dashboard")
        );
    }

    @ParameterizedTest
    @MethodSource("provideHostAndIdForAppointment")
    void getAppointmentUrl_ShouldBuildCorrectUrl(String host, String appointmentId, String expectedUrl) {
        // When
        String result = NotificationUtils.getAppointmentUrl(host, appointmentId);

        // Then
        assertEquals(expectedUrl, result);
    }

    private static Stream<Arguments> provideHostAndIdForAppointment() {
        return Stream.of(
                Arguments.of("https://medilabo.fr", "app-123", "https://medilabo.fr/appointments/app-123"),
                Arguments.of("http://localhost:8080", "456", "http://localhost:8080/appointments/456"),
                Arguments.of("https://api.medilabo.com", "appt-789", "https://api.medilabo.com/appointments/appt-789")
        );
    }

    @ParameterizedTest
    @MethodSource("provideHostAndIdForNote")
    void getNoteUrl_ShouldBuildCorrectUrl(String host, String noteUuid, String expectedUrl) {
        // When
        String result = NotificationUtils.getNoteUrl(host, noteUuid);

        // Then
        assertEquals(expectedUrl, result);
    }

    private static Stream<Arguments> provideHostAndIdForNote() {
        return Stream.of(
                Arguments.of("https://medilabo.fr", "note-123", "https://medilabo.fr/notes/note-123"),
                Arguments.of("http://localhost:8080", "456", "http://localhost:8080/notes/456"),
                Arguments.of("https://api.medilabo.com", "note-uuid-789", "https://api.medilabo.com/notes/note-uuid-789")
        );
    }

    @ParameterizedTest
    @MethodSource("provideHostForResults")
    void getResultsUrl_ShouldBuildCorrectUrl(String host, String expectedUrl) {
        // When
        String result = NotificationUtils.getResultsUrl(host);

        // Then
        assertEquals(expectedUrl, result);
    }

    private static Stream<Arguments> provideHostForResults() {
        return Stream.of(
                Arguments.of("https://medilabo.fr", "https://medilabo.fr/results"),
                Arguments.of("http://localhost:8080", "http://localhost:8080/results"),
                Arguments.of("https://api.medilabo.com", "https://api.medilabo.com/results")
        );
    }

    @ParameterizedTest
    @MethodSource("provideHostForMessages")
    void getMessagesUrl_ShouldBuildCorrectUrl(String host, String expectedUrl) {
        // When
        String result = NotificationUtils.getMessagesUrl(host);

        // Then
        assertEquals(expectedUrl, result);
    }

    private static Stream<Arguments> provideHostForMessages() {
        return Stream.of(
                Arguments.of("https://medilabo.fr", "https://medilabo.fr/messages"),
                Arguments.of("http://localhost:8080", "http://localhost:8080/messages"),
                Arguments.of("https://api.medilabo.com", "https://api.medilabo.com/messages")
        );
    }

    @ParameterizedTest
    @MethodSource("provideHostAndIdForConversation")
    void getConversationUrl_ShouldBuildCorrectUrl(String host, String conversationId, String expectedUrl) {
        // When
        String result = NotificationUtils.getConversationUrl(host, conversationId);

        // Then
        assertEquals(expectedUrl, result);
    }

    private static Stream<Arguments> provideHostAndIdForConversation() {
        return Stream.of(
                Arguments.of("https://medilabo.fr", "conv-123", "https://medilabo.fr/messages/conv-123"),
                Arguments.of("http://localhost:8080", "456", "http://localhost:8080/messages/456"),
                Arguments.of("https://api.medilabo.com", "conversation-789", "https://api.medilabo.com/messages/conversation-789")
        );
    }

    @ParameterizedTest
    @MethodSource("provideHostForRiskAssessment")
    void getRiskAssessmentUrl_ShouldBuildCorrectUrl(String host, String expectedUrl) {
        // When
        String result = NotificationUtils.getRiskAssessmentUrl(host);

        // Then
        assertEquals(expectedUrl, result);
    }

    private static Stream<Arguments> provideHostForRiskAssessment() {
        return Stream.of(
                Arguments.of("https://medilabo.fr", "https://medilabo.fr/risk-assessment"),
                Arguments.of("http://localhost:8080", "http://localhost:8080/risk-assessment"),
                Arguments.of("https://api.medilabo.com", "https://api.medilabo.com/risk-assessment")
        );
    }

    @Test
    void getAppointmentUrl_ShouldHandleNullAppointmentId() {
        // Given
        String host = "https://medilabo.fr";

        // When
        String result = NotificationUtils.getAppointmentUrl(host, null);

        // Then
        assertEquals("https://medilabo.fr/appointments/null", result);
    }

    @Test
    void getNoteUrl_ShouldHandleNullNoteUuid() {
        // Given
        String host = "https://medilabo.fr";

        // When
        String result = NotificationUtils.getNoteUrl(host, null);

        // Then
        assertEquals("https://medilabo.fr/notes/null", result);
    }

    @Test
    void getConversationUrl_ShouldHandleNullConversationId() {
        // Given
        String host = "https://medilabo.fr";

        // When
        String result = NotificationUtils.getConversationUrl(host, null);

        // Then
        assertEquals("https://medilabo.fr/messages/null", result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void getPatientDashboardUrl_ShouldHandleInvalidHost(String invalidHost) {
        // When
        String result = NotificationUtils.getPatientDashboardUrl(invalidHost);

        // Then
        assertTrue(result.contains("/dashboard"));
    }

    @Test
    void privateConstructor_ShouldNotBeInstantiable() throws Exception {
        // Vérifier que le constructeur privé existe
        java.lang.reflect.Constructor<NotificationUtils> constructor =
                NotificationUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // Vérifier qu'on ne peut pas instancier la classe (constructeur privé)
        assertThrows(IllegalAccessException.class, () -> {
            NotificationUtils.class.newInstance();
        });
    }

    @Test
    void allUrlMethods_ShouldWorkWithVariousHostFormats() {
        // Given
        String[] hosts = {
                "https://medilabo.fr",
                "http://localhost:8080",
                "https://api.medilabo.com",
                "medilabo.fr", // sans protocole
                "localhost",   // sans protocole ni domaine
                "",            // chaîne vide
                null           // null
        };

        for (String host : hosts) {
            // When - Then (pas d'exception)
            assertDoesNotThrow(() -> NotificationUtils.getPatientDashboardUrl(host));
            assertDoesNotThrow(() -> NotificationUtils.getResultsUrl(host));
            assertDoesNotThrow(() -> NotificationUtils.getMessagesUrl(host));
            assertDoesNotThrow(() -> NotificationUtils.getRiskAssessmentUrl(host));
            assertDoesNotThrow(() -> NotificationUtils.getVerificationUrl(host, "token"));
            assertDoesNotThrow(() -> NotificationUtils.getResetPasswordUrl(host, "token"));
            assertDoesNotThrow(() -> NotificationUtils.getAppointmentUrl(host, "id"));
            assertDoesNotThrow(() -> NotificationUtils.getNoteUrl(host, "uuid"));
            assertDoesNotThrow(() -> NotificationUtils.getConversationUrl(host, "conv-id"));
        }
    }

    @Test
    void randomUUID_ShouldBeConsistent() {
        // Vérifier que le Supplier est bien défini et fonctionne à chaque appel
        for (int i = 0; i < 100; i++) {
            String uuid = NotificationUtils.randomUUID.get();
            assertNotNull(uuid);
            assertDoesNotThrow(() -> UUID.fromString(uuid));
        }
    }
}