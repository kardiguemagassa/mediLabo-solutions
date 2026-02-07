package com.openclassrooms.notificationservice.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.notificationservice.dto.SendMessageRequest;
import com.openclassrooms.notificationservice.model.Message;
import com.openclassrooms.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour NotificationResource.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@WebMvcTest(NotificationResource.class)
@DisplayName("NotificationResource Tests")
class NotificationResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    private Message sampleMessage;

    @BeforeEach
    void setUp() {
        sampleMessage = Message.builder()
                .messageId(1L)
                .messageUuid("msg-uuid-123")
                .conversationId("conv-uuid-456")
                .senderUuid("sender-uuid")
                .senderName("Dr. Jean Dupont")
                .senderEmail("jean.dupont@medilabo.fr")
                .senderRole("DOCTOR")
                .receiverUuid("receiver-uuid")
                .receiverName("Marie Martin")
                .receiverEmail("marie.martin@patient.fr")
                .receiverRole("PATIENT")
                .subject("Résultats d'analyses")
                .message("Vos résultats sont disponibles.")
                .status("READ")
                .createdAt("2026-02-09T10:30:00Z")
                .build();
    }

    @Nested
    @DisplayName("POST /api/messages - Envoyer un message")
    class SendMessageTests {

        @Test
        @DisplayName("Devrait envoyer un message avec succès - 201 Created")
        void shouldSendMessageSuccessfully() throws Exception {
            // Given
            SendMessageRequest request = SendMessageRequest.builder()
                    .receiverEmail("marie.martin@patient.fr")
                    .subject("Résultats d'analyses")
                    .message("Vos résultats sont disponibles.")
                    .build();

            when(notificationService.sendMessage(
                    anyString(), anyString(), anyString(), any(), anyString(),
                    anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(sampleMessage));

            // When - Start async request
            MvcResult mvcResult = mockMvc.perform(post("/api/messages")
                            .with(jwt()
                                    .jwt(jwt -> jwt
                                            .subject("sender-uuid")
                                            .claim("email", "jean.dupont@medilabo.fr")
                                            .claim("name", "Dr. Jean Dupont")
                                            .claim("role", "DOCTOR")))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            // Then - Wait for async and verify
            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Message envoyé avec succès"))
                    .andExpect(jsonPath("$.data.message.messageUuid").value("msg-uuid-123"));
        }

        @Test
        @DisplayName("Devrait retourner 400 si email destinataire manquant")
        void shouldReturn400WhenReceiverEmailMissing() throws Exception {
            // Given
            SendMessageRequest request = SendMessageRequest.builder()
                    .subject("Sujet")
                    .message("Message")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/messages")
                            .with(jwt().jwt(jwt -> jwt.subject("user-uuid")))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devrait retourner 403 si non authentifié (CSRF bloque avant auth)")
        void shouldReturn403WhenNotAuthenticated() throws Exception {
            // Given
            SendMessageRequest request = SendMessageRequest.builder()
                    .receiverEmail("test@email.com")
                    .subject("Sujet")
                    .message("Message")
                    .build();

            // When & Then - Sans JWT ni CSRF, Spring Security retourne 403
            mockMvc.perform(post("/api/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Devrait retourner 401 si authentification invalide (avec CSRF)")
        void shouldReturn401WhenAuthenticationInvalid() throws Exception {
            // Given
            SendMessageRequest request = SendMessageRequest.builder()
                    .receiverEmail("test@email.com")
                    .subject("Sujet")
                    .message("Message")
                    .build();

            // When & Then - Avec CSRF mais sans JWT valide
            mockMvc.perform(post("/api/messages")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/messages - Récupérer les messages")
    class GetMessagesTests {

        @Test
        @DisplayName("Devrait récupérer tous les messages - 200 OK")
        void shouldGetAllMessages() throws Exception {
            // Given
            when(notificationService.getMessages(anyString()))
                    .thenReturn(List.of(sampleMessage));
            when(notificationService.getUnreadCount(anyString()))
                    .thenReturn(3);

            // When & Then
            mockMvc.perform(get("/api/messages")
                            .with(jwt().jwt(jwt -> jwt.subject("user-uuid"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.messages").isArray())
                    .andExpect(jsonPath("$.data.messages[0].messageUuid").value("msg-uuid-123"))
                    .andExpect(jsonPath("$.data.unreadCount").value(3));
        }

        @Test
        @DisplayName("Devrait retourner liste vide si aucun message")
        void shouldReturnEmptyListWhenNoMessages() throws Exception {
            // Given
            when(notificationService.getMessages(anyString()))
                    .thenReturn(List.of());
            when(notificationService.getUnreadCount(anyString()))
                    .thenReturn(0);

            // When & Then
            mockMvc.perform(get("/api/messages")
                            .with(jwt().jwt(jwt -> jwt.subject("user-uuid"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.messages").isEmpty())
                    .andExpect(jsonPath("$.data.unreadCount").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/messages/{conversationId} - Récupérer une conversation")
    class GetConversationTests {

        @Test
        @DisplayName("Devrait récupérer une conversation - 200 OK")
        void shouldGetConversation() throws Exception {
            // Given
            when(notificationService.getConversation(anyString(), anyString()))
                    .thenReturn(List.of(sampleMessage));

            // When & Then
            mockMvc.perform(get("/api/messages/conv-uuid-456")
                            .with(jwt().jwt(jwt -> jwt.subject("user-uuid"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.conversation").isArray())
                    .andExpect(jsonPath("$.data.conversation[0].conversationId").value("conv-uuid-456"));
        }
    }

    @Nested
    @DisplayName("GET /api/messages/unread/count - Compteur messages non lus")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Devrait retourner le compteur de messages non lus")
        void shouldReturnUnreadCount() throws Exception {
            // Given
            when(notificationService.getUnreadCount(anyString()))
                    .thenReturn(7);

            // When & Then
            mockMvc.perform(get("/api/messages/unread/count")
                            .with(jwt().jwt(jwt -> jwt.subject("user-uuid"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.unreadCount").value(7));
        }
    }
}