package com.openclassrooms.notesservice.service;

import com.openclassrooms.notesservice.dto.CommentRequest;
import com.openclassrooms.notesservice.dto.CommentResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service de gestion des commentaires sur les notes médicales (Full Réactif).
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
public interface NoteCommentService {

    /**
     * Ajoute un commentaire à une note.
     */
    Mono<CommentResponse> addComment(String noteUuid, CommentRequest request, Jwt jwt);

    /**
     * Liste tous les commentaires d'une note.
     */
    Flux<CommentResponse> getComments(String noteUuid);

    /**
     * Met à jour un commentaire.
     */
    Mono<CommentResponse> updateComment(String noteUuid, String commentUuid, CommentRequest request, Jwt jwt);

    /**
     * Supprime un commentaire.
     */
    Mono<Void> deleteComment(String noteUuid, String commentUuid, Jwt jwt);
}