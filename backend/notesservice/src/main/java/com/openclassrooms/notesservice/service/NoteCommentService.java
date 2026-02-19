package com.openclassrooms.notesservice.service;

import com.openclassrooms.notesservice.dto.CommentRequest;
import com.openclassrooms.notesservice.dto.CommentResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Service de gestion des commentaires sur les notes médicales.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-07
 */
public interface NoteCommentService {

    /**
     * Ajoute un commentaire à une note.
     *
     * @param noteUuid UUID de la note
     * @param request  Contenu du commentaire
     * @param jwt      Token JWT de l'utilisateur
     * @return CommentResponse avec les informations du commentaire
     */
    CommentResponse addComment(String noteUuid, CommentRequest request, Jwt jwt);

    /**
     * Liste tous les commentaires d'une note.
     *
     * @param noteUuid UUID de la note
     * @return Liste des commentaires
     */
    List<CommentResponse> getComments(String noteUuid);

    /**
     * Met à jour un commentaire.
     *
     * @param noteUuid    UUID de la note
     * @param commentUuid UUID du commentaire
     * @param request     Nouveau contenu
     * @param jwt         Token JWT de l'utilisateur
     * @return CommentResponse mis à jour
     */
    CommentResponse updateComment(String noteUuid, String commentUuid, CommentRequest request, Jwt jwt);

    /**
     * Supprime un commentaire.
     *
     * @param noteUuid    UUID de la note
     * @param commentUuid UUID du commentaire
     * @param jwt         Token JWT de l'utilisateur
     */
    void deleteComment(String noteUuid, String commentUuid, Jwt jwt);
}