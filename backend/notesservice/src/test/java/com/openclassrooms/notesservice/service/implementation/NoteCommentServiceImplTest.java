package com.openclassrooms.notesservice.service.implementation;

import com.openclassrooms.notesservice.dto.CommentRequest;
import com.openclassrooms.notesservice.dto.PatientInfo;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.model.Comment;
import com.openclassrooms.notesservice.model.Note;
import com.openclassrooms.notesservice.repository.NoteRepository;
import com.openclassrooms.notesservice.service.PatientServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoteCommentServiceImpl Unit Tests")
class NoteCommentServiceImplTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private PatientServiceClient patientServiceClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NoteCommentServiceImpl noteCommentService;

    private Note testNote;
    private Comment testComment;
    private Jwt mockJwt;
    private PatientInfo patientInfo;

    private static final String NOTE_UUID = "note-uuid-123";
    private static final String COMMENT_UUID = "comment-uuid-456";
    private static final String AUTHOR_UUID = "author-uuid-789";
    private static final String PATIENT_UUID = "patient-uuid-000";
    private static final String PRACTITIONER_UUID = "practitioner-uuid-111";

    @BeforeEach
    void setUp() {
        // Setup JWT mock
        mockJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(AUTHOR_UUID)
                .claim("firstName", "Jean")
                .claim("lastName", "Dupont")
                .claim("authorities", java.util.List.of("ROLE_PRACTITIONER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // Setup comment
        testComment = Comment.builder()
                .commentUuid(COMMENT_UUID)
                .content("Commentaire de test")
                .authorUuid(AUTHOR_UUID)
                .authorName("Jean Dupont")
                .authorRole("PRACTITIONER")
                .edited(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup note with comments list
        testNote = Note.builder()
                .id("mongo-id")
                .noteUuid(NOTE_UUID)
                .patientUuid(PATIENT_UUID)
                .practitionerUuid(PRACTITIONER_UUID)
                .practitionerName("Dr. Martin")
                .content("Note de test")
                .active(true)
                .comments(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup patient info
        patientInfo = PatientInfo.builder()
                .patientUuid(PATIENT_UUID)
                .userInfo(new PatientInfo.UserInfo(
                        "Patient",
                        "Test",
                        "patient@test.com",
                        null,
                        null,
                        null
                ))
                .build();
    }

    private void setupPatientServiceClientMock() {
        when(patientServiceClient.getPatientContactInfo(anyString()))
                .thenReturn(Mono.just(patientInfo));
    }

    @Nested
    @DisplayName("addComment() Tests")
    class AddCommentTests {

        @Test
        @DisplayName("Should add comment successfully")
        void shouldAddCommentSuccessfully() {
            // Given
            setupPatientServiceClientMock();
            CommentRequest request = CommentRequest.builder()
                    .content("Nouveau commentaire")
                    .build();

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));
            when(noteRepository.save(any(Note.class))).thenReturn(testNote);

            // When & Then
            StepVerifier.create(noteCommentService.addComment(NOTE_UUID, request, mockJwt))
                    .expectNextMatches(response ->
                            response.getContent().equals("Nouveau commentaire") &&
                                    response.getAuthorName().equals("Jean Dupont"))
                    .verifyComplete();

            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("Should error when note not found")
        void shouldErrorWhenNoteNotFound() {
            // Given
            CommentRequest request = CommentRequest.builder()
                    .content("Commentaire")
                    .build();

            when(noteRepository.findByNoteUuidAndActiveTrue(anyString()))
                    .thenReturn(Optional.empty());

            // When & Then
            StepVerifier.create(noteCommentService.addComment("unknown-uuid", request, mockJwt))
                    .expectErrorMatches(err -> err instanceof ApiException &&
                            err.getMessage().contains("Note non trouvée"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("getComments() Tests")
    class GetCommentsTests {

        @Test
        @DisplayName("Should return comments for note")
        void shouldReturnCommentsForNote() {
            // Given
            testNote.getComments().add(testComment);

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));

            // When & Then
            StepVerifier.create(noteCommentService.getComments(NOTE_UUID))
                    .expectNextMatches(response ->
                            response.getCommentUuid().equals(COMMENT_UUID))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty flux when no comments")
        void shouldReturnEmptyFluxWhenNoComments() {
            // Given
            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));

            // When & Then
            StepVerifier.create(noteCommentService.getComments(NOTE_UUID))
                    .expectNextCount(0)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("updateComment() Tests")
    class UpdateCommentTests {

        @Test
        @DisplayName("Should update comment when authorized")
        void shouldUpdateCommentWhenAuthorized() {
            // Given
            setupPatientServiceClientMock();
            testNote.getComments().add(testComment);

            CommentRequest request = CommentRequest.builder()
                    .content("Contenu modifié")
                    .build();

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));
            when(noteRepository.save(any(Note.class))).thenReturn(testNote);

            // When & Then
            StepVerifier.create(noteCommentService.updateComment(NOTE_UUID, COMMENT_UUID, request, mockJwt))
                    .expectNextMatches(response ->
                            response.getContent().equals("Contenu modifié") &&
                                    response.getEdited())
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should error when not author")
        void shouldErrorWhenNotAuthor() {
            // Given
            Comment otherComment = testComment.toBuilder()
                    .authorUuid("other-author")
                    .build();
            testNote.getComments().add(otherComment);

            CommentRequest request = CommentRequest.builder()
                    .content("Tentative de modification")
                    .build();

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));

            // When & Then
            StepVerifier.create(noteCommentService.updateComment(NOTE_UUID, COMMENT_UUID, request, mockJwt))
                    .expectErrorMatches(err -> err instanceof ApiException &&
                            err.getMessage().contains("Non autorisé"))
                    .verify();
        }

        @Test
        @DisplayName("Should error when comment not found")
        void shouldErrorWhenCommentNotFound() {
            // Given
            CommentRequest request = CommentRequest.builder()
                    .content("Contenu")
                    .build();

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));

            // When & Then
            StepVerifier.create(noteCommentService.updateComment(NOTE_UUID, "unknown-comment", request, mockJwt))
                    .expectErrorMatches(err -> err instanceof ApiException &&
                            err.getMessage().contains("Commentaire non trouvé"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("deleteComment() Tests")
    class DeleteCommentTests {

        @Test
        @DisplayName("Should delete comment when author")
        void shouldDeleteCommentWhenAuthor() {
            // Given
            setupPatientServiceClientMock();
            testNote.getComments().add(testComment);

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));
            when(noteRepository.save(any(Note.class))).thenReturn(testNote);

            // When & Then
            StepVerifier.create(noteCommentService.deleteComment(NOTE_UUID, COMMENT_UUID, mockJwt))
                    .verifyComplete();

            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("Should delete comment when note practitioner")
        void shouldDeleteCommentWhenNotePractitioner() {
            // Given
            setupPatientServiceClientMock();

            // Comment by another author
            Comment otherComment = testComment.toBuilder()
                    .authorUuid("other-author")
                    .build();
            testNote.getComments().add(otherComment);

            // JWT user is the note's practitioner
            Jwt practitionerJwt = Jwt.withTokenValue("token")
                    .header("alg", "RS256")
                    .subject(PRACTITIONER_UUID)
                    .claim("firstName", "Dr")
                    .claim("lastName", "Martin")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));
            when(noteRepository.save(any(Note.class))).thenReturn(testNote);

            // When & Then
            StepVerifier.create(noteCommentService.deleteComment(NOTE_UUID, COMMENT_UUID, practitionerJwt))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should error when not authorized")
        void shouldErrorWhenNotAuthorized() {
            // Given
            Comment otherComment = testComment.toBuilder()
                    .authorUuid("other-author")
                    .build();
            testNote.getComments().add(otherComment);

            when(noteRepository.findByNoteUuidAndActiveTrue(NOTE_UUID))
                    .thenReturn(Optional.of(testNote));

            // When & Then
            StepVerifier.create(noteCommentService.deleteComment(NOTE_UUID, COMMENT_UUID, mockJwt))
                    .expectErrorMatches(err -> err instanceof ApiException &&
                            err.getMessage().contains("Non autorisé"))
                    .verify();
        }
    }
}