package com.openclassrooms.notesservice.service;

import com.openclassrooms.notesservice.client.PatientServiceClient;
import com.openclassrooms.notesservice.dto.CommentRequest;
import com.openclassrooms.notesservice.dto.CommentResponse;
import com.openclassrooms.notesservice.dto.PatientInfo;
import com.openclassrooms.notesservice.enumeration.EventType;
import com.openclassrooms.notesservice.event.Event;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.model.Comment;
import com.openclassrooms.notesservice.model.Note;
import com.openclassrooms.notesservice.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Service pour la gestion des commentaires sur les notes.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteCommentService {

    private final NoteRepository noteRepository;
    private final PatientServiceClient patientServiceClient;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Ajoute un commentaire à une note.
     *
     * @param noteUuid UUID de la note
     * @param request  Contenu du commentaire
     * @param jwt      Token JWT de l'utilisateur
     * @return CommentResponse avec les informations du commentaire
     */
    public CommentResponse addComment(String noteUuid, CommentRequest request, Jwt jwt) {
        Note note = noteRepository.findByNoteUuidAndActiveTrue(noteUuid)
                .orElseThrow(() -> new ApiException("Note non trouvée: " + noteUuid));

        Comment comment = Comment.builder()
                .commentUuid(UUID.randomUUID().toString())
                .content(request.getContent())
                .authorUuid(jwt.getSubject())
                .authorName(extractName(jwt))
                .authorRole(extractRole(jwt))
                .authorImageUrl(jwt.getClaimAsString("imageUrl"))
                .edited(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        note.addComment(comment);
        noteRepository.save(note);

        log.info("Commentaire ajouté à la note: {} par: {}", noteUuid, comment.getAuthorName());

        // Publier l'événement Kafka avec les infos patient
        publishCommentCreatedEvent(note, comment);

        return mapToCommentResponse(comment);
    }

    /**
     * Liste tous les commentaires d'une note.
     *
     * @param noteUuid UUID de la note
     * @return Liste des commentaires
     */
    public List<CommentResponse> getComments(String noteUuid) {
        Note note = noteRepository.findByNoteUuidAndActiveTrue(noteUuid)
                .orElseThrow(() -> new ApiException("Note non trouvée: " + noteUuid));

        return note.getComments().stream()
                .map(this::mapToCommentResponse)
                .toList();
    }

    /**
     * Met à jour un commentaire.
     *
     * @param noteUuid    UUID de la note
     * @param commentUuid UUID du commentaire
     * @param request     Nouveau contenu
     * @param jwt         Token JWT de l'utilisateur
     * @return CommentResponse mis à jour
     */
    public CommentResponse updateComment(String noteUuid, String commentUuid, CommentRequest request, Jwt jwt) {
        Note note = noteRepository.findByNoteUuidAndActiveTrue(noteUuid)
                .orElseThrow(() -> new ApiException("Note non trouvée: " + noteUuid));

        Comment comment = note.findComment(commentUuid);
        if (comment == null) {
            throw new ApiException("Commentaire non trouvé: " + commentUuid);
        }

        // Vérifier que l'utilisateur est l'auteur du commentaire
        if (!comment.getAuthorUuid().equals(jwt.getSubject())) {
            throw new ApiException("Non autorisé à modifier ce commentaire");
        }

        comment.setContent(request.getContent());
        comment.setEdited(true);
        comment.setUpdatedAt(LocalDateTime.now());

        noteRepository.save(note);

        log.info("Commentaire modifié: {} sur la note: {}", commentUuid, noteUuid);

        return mapToCommentResponse(comment);
    }

    /**
     * Supprime un commentaire.
     *
     * @param noteUuid    UUID de la note
     * @param commentUuid UUID du commentaire
     * @param jwt         Token JWT de l'utilisateur
     */
    public void deleteComment(String noteUuid, String commentUuid, Jwt jwt) {
        Note note = noteRepository.findByNoteUuidAndActiveTrue(noteUuid)
                .orElseThrow(() -> new ApiException("Note non trouvée: " + noteUuid));

        Comment comment = note.findComment(commentUuid);
        if (comment == null) {
            throw new ApiException("Commentaire non trouvé: " + commentUuid);
        }

        // Vérifier les droits (auteur du commentaire ou praticien de la note)
        String userUuid = jwt.getSubject();
        if (!comment.getAuthorUuid().equals(userUuid)
                && !note.getPractitionerUuid().equals(userUuid)) {
            throw new ApiException("Non autorisé à supprimer ce commentaire");
        }

        note.removeComment(commentUuid);
        noteRepository.save(note);

        log.info("Commentaire supprimé: {} de la note: {}", commentUuid, noteUuid);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Publie un événement COMMENT_CREATED avec les infos patient.
     */
    private void publishCommentCreatedEvent(Note note, Comment comment) {
        // 1. On lance l'appel asynchrone
        patientServiceClient.getPatientContactInfoAsync(note.getPatientUuid())
                .thenAccept(patientOpt -> {
                    // 2. Cette partie s'exécutera toute seule quand le résultat arrivera
                    String patientEmail = null;
                    String patientName = null;

                    if (patientOpt.isPresent()) {
                        PatientInfo patient = patientOpt.get();
                        patientEmail = patient.getEmail();
                        patientName = patient.getFullName();
                    } else {
                        log.warn("Could not fetch patient info for event, patientUuid: {}", note.getPatientUuid());
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("commentUuid", comment.getCommentUuid());
                    data.put("commentPreview", truncateContent(comment.getContent(), 100));
                    data.put("authorName", comment.getAuthorName());
                    data.put("authorRole", comment.getAuthorRole());

                    Event event = Event.builder()
                            .type(EventType.COMMENT_CREATED)
                            .noteUuid(note.getNoteUuid())
                            .patientUuid(note.getPatientUuid())
                            .patientEmail(patientEmail)
                            .patientName(patientName)
                            .practitionerUuid(comment.getAuthorUuid())
                            .practitionerName(comment.getAuthorName())
                            .practitionerRole(comment.getAuthorRole())
                            .data(data)
                            .build();

                    eventPublisher.publishEvent(event);
                    log.debug("COMMENT_CREATED event published for note: {}", note.getNoteUuid());
                })
                .exceptionally(ex -> {
                    log.error("Erreur asynchrone lors de la récupération patient : {}", ex.getMessage());
                    return null;
                });
    }

    /**
     * Mappe un Comment vers CommentResponse.
     */
    private CommentResponse mapToCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .commentUuid(comment.getCommentUuid())
                .content(comment.getContent())
                .authorUuid(comment.getAuthorUuid())
                .authorName(comment.getAuthorName())
                .authorRole(comment.getAuthorRole())
                .authorImageUrl(comment.getAuthorImageUrl())
                .edited(comment.getEdited())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /**
     * Extrait le nom de l'utilisateur depuis le JWT.
     */
    private String extractName(Jwt jwt) {
        String firstName = jwt.getClaimAsString("firstName");
        String lastName = jwt.getClaimAsString("lastName");
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        String name = jwt.getClaimAsString("name");
        return name != null ? name : "Unknown";
    }

    /**
     * Extrait le rôle principal du JWT.
     */
    private String extractRole(Jwt jwt) {
        var authorities = jwt.getClaimAsStringList("authorities");
        if (authorities != null && !authorities.isEmpty()) {
            return authorities.stream()
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.replace("ROLE_", ""))
                    .findFirst()
                    .orElse("USER");
        }
        return "USER";
    }

    /**
     * Tronque le contenu pour l'aperçu.
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength - 3) + "...";
    }
}