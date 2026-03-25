package com.openclassrooms.notesservice.service.implementation;

import com.openclassrooms.notesservice.dto.CommentRequest;
import com.openclassrooms.notesservice.dto.CommentResponse;
import com.openclassrooms.notesservice.dto.PatientInfo;
import com.openclassrooms.notesservice.enumeration.EventType;
import com.openclassrooms.notesservice.event.Event;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.model.Comment;
import com.openclassrooms.notesservice.model.Note;
import com.openclassrooms.notesservice.repository.NoteRepository;
import com.openclassrooms.notesservice.service.NoteCommentService;
import com.openclassrooms.notesservice.service.PatientServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implémentation réactive du service de gestion des commentaires.
 * ARCHITECTURE RÉACTIVE:
 * - Mono.fromCallable() : Encapsule les appels MongoDB bloquants
 * - subscribeOn(Schedulers.boundedElastic()) : Exécute sur thread-pool élastique
 * - Publication d'événements Kafka de manière réactive
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteCommentServiceImpl implements NoteCommentService {

    private final NoteRepository noteRepository;
    private final PatientServiceClient patientServiceClient;
    private final ApplicationEventPublisher eventPublisher;


    /**
     * Ajoute un commentaire à une note.
     * 1. Récupération de la note
     * 2. Construction du commentaire
     * 3. Ajout à la note et sauvegarde
     * 4. Publication de l'événement Kafka (async)
     * 5. Retour du CommentResponse
     */
    @Override
    public Mono<CommentResponse> addComment(String noteUuid, CommentRequest request, Jwt jwt) {
        log.debug("Adding comment to note: {}", noteUuid);

        return findNoteByUuid(noteUuid)
                .flatMap(note -> {
                    /** Construction du commentaire */
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

                    /** Ajout à la note */
                    note.addComment(comment);

                    /** Sauvegarde MongoDB */
                    return Mono.fromCallable(() -> {
                                noteRepository.save(note);
                                log.info("Commentaire ajouté à la note: {} par: {}", noteUuid, comment.getAuthorName());
                                return comment;
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            /** Publier l'événement de manière réactive (fire and forget) */
                            .doOnSuccess(savedComment -> publishCommentCreatedEvent(note, savedComment))
                            .map(this::mapToCommentResponse);
                });
    }

    /**
     * Liste tous les commentaires d'une note.
     */
    @Override
    public Flux<CommentResponse> getComments(String noteUuid) {
        log.debug("Getting comments for note: {}", noteUuid);

        return findNoteByUuid(noteUuid)
                /** Convertir la liste de commentaires en Flux */
                .flatMapMany(note -> Flux.fromIterable(note.getComments()))
                /** Mapper chaque Comment vers CommentResponse */
                .map(this::mapToCommentResponse);
    }

    /**
     * Met à jour un commentaire.
     * 1. Récupération de la note
     * 2. Recherche du commentaire
     * 3. Vérification des droits (auteur uniquement)
     * 4. Mise à jour et sauvegarde
     */
    @Override
    public Mono<CommentResponse> updateComment(String noteUuid, String commentUuid, CommentRequest request, Jwt jwt) {
        log.debug("Updating comment: {} on note: {}", commentUuid, noteUuid);

        return findNoteByUuid(noteUuid)
                .flatMap(note -> {
                    /** Recherche du commentaire */
                    Comment comment = note.findComment(commentUuid);
                    if (comment == null) {
                        return Mono.error(new ApiException("Commentaire non trouvé: " + commentUuid));
                    }

                    /** Vérification des droits */
                    if (!comment.getAuthorUuid().equals(jwt.getSubject())) {
                        return Mono.error(new ApiException("Non autorisé à modifier ce commentaire"));
                    }

                    // Mise à jour
                    comment.setContent(request.getContent());
                    comment.setEdited(true);
                    comment.setUpdatedAt(LocalDateTime.now());

                    /** Sauvegarde */
                    return Mono.fromCallable(() -> {
                                noteRepository.save(note);
                                log.info("Commentaire modifié: {} sur la note: {}", commentUuid, noteUuid);
                                return mapToCommentResponse(comment);
                            })
                            .doOnSuccess(response -> publishCommentUpdatedEvent(note, comment))
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }



    /**
     * Supprime un commentaire.
     * 1. Récupération de la note
     * 2. Recherche du commentaire
     * 3. Vérification des droits (auteur ou praticien de la note)
     * 4. Suppression et sauvegarde
     */
    @Override
    public Mono<Void> deleteComment(String noteUuid, String commentUuid, Jwt jwt) {
        log.debug("Deleting comment: {} from note: {}", commentUuid, noteUuid);

        return findNoteByUuid(noteUuid)
                .flatMap(note -> {

                    Comment comment = note.findComment(commentUuid);
                    if (comment == null) {
                        return Mono.error(new ApiException("Commentaire non trouvé: " + commentUuid));
                    }

                    /** Vérification des droits */
                    String userUuid = jwt.getSubject();
                    if (!comment.getAuthorUuid().equals(userUuid)
                            && !note.getPractitionerUuid().equals(userUuid)) {
                        return Mono.error(new ApiException("Non autorisé à supprimer ce commentaire"));
                    }

                    note.removeComment(commentUuid);

                    return Mono.fromCallable(() -> {
                                noteRepository.save(note);
                                log.info("Commentaire supprimé: {} de la note: {}", commentUuid, noteUuid);
                                return note;
                            })
                            .doOnSuccess(n -> publishCommentDeletedEvent(n, comment))
                            .subscribeOn(Schedulers.boundedElastic());
                })
                .then();
    }

    /**
     * Recherche une note par UUID.
     * Méthode utilitaire pour éviter la duplication.
     */
    private Mono<Note> findNoteByUuid(String noteUuid) {
        return Mono.fromCallable(() -> noteRepository.findByNoteUuidAndActiveTrue(noteUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new ApiException("Note non trouvée: " + noteUuid))));
    }

    /**
     * Publie un événement COMMENT_CREATED avec les infos patient.
     * Exécution asynchrone (fire and forget).
     */
    private void publishCommentCreatedEvent(Note note, Comment comment) {
        patientServiceClient.getPatientContactInfo(note.getPatientUuid())
                .subscribe(
                        patient -> {
                            Map<String, Object> data = buildEventData(note, comment, patient);
                            Event event = Event.builder()
                                    .eventType(EventType.COMMENT_CREATED)
                                    .data(data)
                                    .build();
                            eventPublisher.publishEvent(event);
                            log.debug("COMMENT_CREATED event published for note: {}", note.getNoteUuid());
                        },
                        error -> log.error("Erreur lors de la récupération patient pour event: {}", error.getMessage()),
                        () -> log.debug("No patient info found for event, skipping notification")
                );
    }

    /**
     * Construit les données de l'événement.
     */
    private Map<String, Object> buildEventData(Note note, Comment comment, PatientInfo patient) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", patient != null ? patient.getFullName() : null);
        data.put("email", patient != null ? patient.getEmail() : null);
        data.put("recordNumber", note.getNoteUuid());
        data.put("subject", "Note médicale - " + note.getPatientUuid());
        data.put("senderName", comment.getAuthorName());
        data.put("date", LocalDateTime.now().toString());
        data.put("comment", truncateContent(comment.getContent(), 200));
        return data;
    }

    private void publishCommentUpdatedEvent(Note note, Comment comment) {
        patientServiceClient.getPatientContactInfo(note.getPatientUuid())
                .subscribe(
                        patient -> {
                            Map<String, Object> data = buildCommentEventData(note, comment, patient, "modifié");
                            Event event = Event.builder()
                                    .eventType(EventType.COMMENT_UPDATED)
                                    .data(data)
                                    .build();
                            eventPublisher.publishEvent(event);
                            log.debug("COMMENT_UPDATED event published for note: {}", note.getNoteUuid());
                        },
                        error -> log.error("Erreur lors de la récupération patient pour event: {}", error.getMessage())
                );
    }

    private void publishCommentDeletedEvent(Note note, Comment comment) {
        patientServiceClient.getPatientContactInfo(note.getPatientUuid())
                .subscribe(
                        patient -> {
                            Map<String, Object> data = buildCommentEventData(note, comment, patient, "supprimé");
                            Event event = Event.builder()
                                    .eventType(EventType.COMMENT_DELETED)
                                    .data(data)
                                    .build();
                            eventPublisher.publishEvent(event);
                            log.debug("COMMENT_DELETED event published for note: {}", note.getNoteUuid());
                        },
                        error -> log.error("Erreur lors de la récupération patient pour event: {}", error.getMessage())
                );
    }

    private Map<String, Object> buildCommentEventData(Note note, Comment comment, PatientInfo patient, String action) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", patient != null ? patient.getFullName() : null);
        data.put("email", patient != null ? patient.getEmail() : null);
        data.put("recordNumber", note.getNoteUuid());
        data.put("subject", "Commentaire " + action);
        data.put("senderName", comment.getAuthorName());
        data.put("date", LocalDateTime.now().toString());
        data.put("comment", truncateContent(comment.getContent(), 200));
        return data;
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