package com.openclassrooms.notificationservice.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.notificationservice.dtorequest.MessageRequest;
import com.openclassrooms.notificationservice.dtorequest.UserRequest;
import com.openclassrooms.notificationservice.dtoresponse.MessageResponse;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests réactifs pour NotificationResource (Mono/Flux)
 */
@WebMvcTest(NotificationResource.class)
@DisplayName("NotificationResource Tests (réactif)")
class NotificationResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    private MessageResponse messageResponse;

    @BeforeEach
    void setUp() {
        messageResponse = MessageResponse.builder()
                .messageUuid("msg-uuid-123")
                .conversationId("conv-uuid-456")
                .subject("Résultats d'analyses")
                .message("Vos résultats sont disponibles.")
                .status("READ")
                .createdAt("2026-02-09T10:30:00Z")
                .build();
    }

    // ==================== SEND MESSAGE ====================
    @Nested
    @DisplayName("POST /api/notifications/messages - Envoyer un message")
    class SendMessageTests {

        @Test
        @DisplayName("Devrait envoyer un message avec succès - 201 Created")
        void shouldSendMessageSuccessfully() throws Exception {
            MessageRequest request = MessageRequest.builder()
                    .receiverEmail("marie.martin@patient.fr")
                    .subject("Résultats d'analyses")
                    .message("Vos résultats sont disponibles.")
                    .build();

            when(notificationService.sendMessage(any(MessageRequest.class), any(UserRequest.class)))
                    .thenReturn(Mono.just(messageResponse));

            MvcResult mvcResult = mockMvc.perform(post("/api/notifications/messages")
                            .with(jwt().jwt(jwt -> jwt.subject("sender-uuid")
                                    .claim("email", "jean.dupont@medilabo.fr")
                                    .claim("firstName", "Jean")
                                    .claim("lastName", "Dupont")
                                    .claim("role", "DOCTOR")))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.message.messageUuid").value("msg-uuid-123"));
        }

        @Test
        @DisplayName("Devrait retourner 400 si email destinataire manquant")
        void shouldReturn400WhenReceiverEmailMissing() throws Exception {
            MessageRequest request = MessageRequest.builder()
                    .subject("Sujet")
                    .message("Message")
                    .build();

            mockMvc.perform(post("/api/notifications/messages")
                            .with(jwt().jwt(jwt -> jwt.subject("user-uuid")))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Devrait retourner 403 si non authentifié")
        void shouldReturn403WhenNotAuthenticated() throws Exception {
            MessageRequest request = MessageRequest.builder()
                    .receiverEmail("test@email.com")
                    .subject("Sujet")
                    .message("Message")
                    .build();

            mockMvc.perform(post("/api/notifications/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== REPLY MESSAGE ====================
    @Nested
    @DisplayName("POST /api/notifications/reply - Répondre à un message")
    class ReplyMessageTests {

        @Test
        @DisplayName("Devrait répondre à un message avec succès")
        void shouldReplyMessageSuccessfully() throws Exception {
            // MessageRequest exige subject et receiverEmail via @NotBlank
            // Pour une réponse, on doit quand même les fournir (même si logiquement inutiles)
            MessageRequest request = MessageRequest.builder()
                    .conversationId("conv-uuid-456")
                    .receiverEmail("marie.martin@patient.fr")
                    .subject("Re: Résultats d'analyses")
                    .message("Merci pour les résultats.")
                    .build();

            when(notificationService.sendMessage(any(MessageRequest.class), any(UserRequest.class)))
                    .thenReturn(Mono.just(messageResponse));

            MvcResult mvcResult = mockMvc.perform(post("/api/notifications/reply")
                            .with(jwt().jwt(jwt -> jwt.subject("sender-uuid")
                                    .claim("email", "jean.dupont@medilabo.fr")
                                    .claim("firstName", "Jean")
                                    .claim("lastName", "Dupont")))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.message.messageUuid").value("msg-uuid-123"));
        }

//        @Test
//        @DisplayName("Devrait retourner 400 si conversationId manquant pour reply")
//        void shouldReturn400WhenConversationIdMissing() throws Exception {
//            // Pour une réponse, conversationId devrait être obligatoire
//            MessageRequest request = MessageRequest.builder()
//                    .receiverEmail("marie.martin@patient.fr")
//                    .subject("Re: Sujet")
//                    .message("Réponse sans conversationId")
//                    // conversationId manquant
//                    .build();
//
//            // Ce test dépend de la validation dans MessageRequest
//            // Si conversationId n'est pas @NotBlank, ce test échouera
//            mockMvc.perform(post("/api/notifications/reply")
//                            .with(jwt().jwt(jwt -> jwt.subject("user-uuid")))
//                            .with(csrf())
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().isBadRequest()); // Ou isBadRequest() selon ta validation
//        }
    }

    // ==================== GET MESSAGES ====================
    @Nested
    @DisplayName("GET /api/notifications/messages - Récupérer les messages")
    class GetMessagesTests {

        @Test
        @DisplayName("Devrait récupérer tous les messages - 200 OK")
        void shouldGetAllMessages() throws Exception {
            when(notificationService.getMessages(anyString()))
                    .thenReturn(Flux.just(messageResponse));
            when(notificationService.getUnreadCount(anyString()))
                    .thenReturn(Mono.just(3));

            MvcResult mvcResult = mockMvc.perform(get("/api/notifications/messages")
                            .with(jwt().jwt(jwt -> jwt.subject("user-uuid"))))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.messages[0].messageUuid").value("msg-uuid-123"))
                    .andExpect(jsonPath("$.data.unreadCount").value(3));
        }

        @Test
        @DisplayName("Devrait retourner liste vide si aucun message")
        void shouldReturnEmptyListWhenNoMessages() throws Exception {
            when(notificationService.getMessages(anyString()))
                    .thenReturn(Flux.empty());
            when(notificationService.getUnreadCount(anyString()))
                    .thenReturn(Mono.just(0));

            MvcResult mvcResult = mockMvc.perform(get("/api/notifications/messages")
                            .with(jwt().jwt(jwt -> jwt.subject("user-uuid"))))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.messages").isEmpty())
                    .andExpect(jsonPath("$.data.unreadCount").value(0));
        }
    }

    // ==================== GET CONVERSATION ====================
    @Nested
    @DisplayName("GET /api/notifications/messages/{conversationId} - Récupérer une conversation")
    class GetConversationTests {

        @Test
        @DisplayName("Devrait récupérer une conversation - 200 OK")
        void shouldGetConversation() throws Exception {
            when(notificationService.getConversation(anyString(), anyString()))
                    .thenReturn(Flux.just(messageResponse));

            MvcResult mvcResult = mockMvc.perform(get("/api/notifications/messages/conv-uuid-456")
                            .with(jwt().jwt(jwt -> jwt.subject("user-uuid"))))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.conversation[0].conversationId").value("conv-uuid-456"));
        }
    }

    // ==================== GET UNREAD COUNT ====================
    @Nested
    @DisplayName("GET /api/notifications/messages/unread/count - Compteur messages non lus")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Devrait retourner le compteur de messages non lus")
        void shouldReturnUnreadCount() throws Exception {
            when(notificationService.getUnreadCount(anyString()))
                    .thenReturn(Mono.just(7));

            MvcResult mvcResult = mockMvc.perform(get("/api/notifications/messages/unread/count")
                            .with(jwt().jwt(jwt -> jwt.subject("user-uuid"))))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.unreadCount").value(7));
        }
    }

    // ==================== MARK AS READ ====================
    @Nested
    @DisplayName("PATCH /api/notifications/messages/{messageId}/read - Marquer comme lu")
    class MarkAsReadTests {

        @Test
        @DisplayName("Devrait marquer un message comme lu")
        void shouldMarkMessageAsRead() throws Exception {
            when(notificationService.markMessageAsRead(anyString(), anyLong()))
                    .thenReturn(Mono.empty());

            MvcResult mvcResult = mockMvc.perform(patch("/api/notifications/messages/123/read")
                            .with(jwt().jwt(jwt -> jwt.subject("user-uuid"))))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.messageId").value(123))
                    .andExpect(jsonPath("$.data.status").value("READ"));
        }
    }
}