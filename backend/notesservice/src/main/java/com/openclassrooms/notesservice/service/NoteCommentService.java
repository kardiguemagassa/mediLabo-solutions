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
    Mono<CommentResponse> addComment(String noteUuid, CommentRequest request, Jwt jwt);
    Flux<CommentResponse> getComments(String noteUuid);
    Mono<CommentResponse> updateComment(String noteUuid, String commentUuid, CommentRequest request, Jwt jwt);
    Mono<Void> deleteComment(String noteUuid, String commentUuid, Jwt jwt);
}