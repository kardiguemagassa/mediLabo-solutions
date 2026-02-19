package com.openclassrooms.notesservice.controller;

import com.openclassrooms.notesservice.domain.Response;
import com.openclassrooms.notesservice.dto.CommentRequest;
import com.openclassrooms.notesservice.dto.CommentResponse;
import com.openclassrooms.notesservice.dto.FileResponse;
import com.openclassrooms.notesservice.service.NoteCommentService;
import com.openclassrooms.notesservice.service.NoteFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static com.openclassrooms.notesservice.constant.Role.*;
import static com.openclassrooms.notesservice.util.RequestUtils.getResponse;
import static org.springframework.http.HttpStatus.*;

/**
 * Controller REST pour la gestion des fichiers et commentaires des notes.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-07
 */
@Tag(name = "Note Attachments", description = "Gestion des fichiers et commentaires des notes médicales")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequestMapping("/api/notes/{noteUuid}")
@RequiredArgsConstructor
public class NoteAttachmentController {

    private final NoteFileService noteFileService;
    private final NoteCommentService noteCommentService;

    // FICHIERS

    @Operation(summary = "Upload un fichier", description = "Attache un fichier à une note médicale")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichier uploadé avec succès"),
            @ApiResponse(responseCode = "400", description = "Fichier invalide"),
            @ApiResponse(responseCode = "404", description = "Note non trouvée"),
            @ApiResponse(responseCode = "413", description = "Fichier trop volumineux")
    })
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> uploadFile(
            @Parameter(description = "UUID de la note") @PathVariable String noteUuid,
            @Parameter(description = "Fichier à uploader") @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        log.info("Upload fichier pour note: {} par: {}", noteUuid, jwt.getSubject());
        FileResponse response = noteFileService.uploadFile(noteUuid, file, jwt);
        return ResponseEntity.ok(getResponse(request, Map.of("file", response), "Fichier uploadé avec succès", OK));
    }

    @Operation(summary = "Liste les fichiers", description = "Récupère tous les fichiers attachés à une note")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des fichiers"),
            @ApiResponse(responseCode = "404", description = "Note non trouvée")
    })
    @GetMapping("/files")
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> getFiles(
            @Parameter(description = "UUID de la note") @PathVariable String noteUuid,
            HttpServletRequest request
    ) {
        log.debug("Liste des fichiers pour note: {}", noteUuid);
        List<FileResponse> files = noteFileService.getFiles(noteUuid);
        return ResponseEntity.ok(getResponse(request, Map.of("files", files, "count", files.size()),
                "Fichiers récupérés avec succès", OK));
    }

    @Operation(summary = "Télécharge un fichier", description = "Télécharge un fichier attaché à une note")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichier téléchargé",
                    content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "404", description = "Fichier non trouvé")
    })
    @GetMapping("/files/{fileUuid}/download")
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "UUID de la note") @PathVariable String noteUuid,
            @Parameter(description = "UUID du fichier") @PathVariable String fileUuid
    ) {
        log.debug("Téléchargement fichier: {} de la note: {}", fileUuid, noteUuid);

        NoteFileService.FileDownload download = noteFileService.downloadFile(noteUuid, fileUuid);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.getFilename() + "\"")
                .body(download.getResource());
    }

    @Operation(summary = "Supprime un fichier", description = "Supprime un fichier attaché à une note")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fichier supprimé"),
            @ApiResponse(responseCode = "403", description = "Non autorisé"),
            @ApiResponse(responseCode = "404", description = "Fichier non trouvé")
    })
    @DeleteMapping("/files/{fileUuid}")
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> deleteFile(
            @Parameter(description = "UUID de la note") @PathVariable String noteUuid,
            @Parameter(description = "UUID du fichier") @PathVariable String fileUuid,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        log.info("Suppression fichier: {} de la note: {} par: {}", fileUuid, noteUuid, jwt.getSubject());
        noteFileService.deleteFile(noteUuid, fileUuid, jwt);
        return ResponseEntity.ok(getResponse(request, Map.of(), "Fichier supprimé avec succès", OK));
    }

    // COMMENTAIRES

    @Operation(summary = "Ajoute un commentaire", description = "Ajoute un commentaire à une note médicale")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commentaire ajouté"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Note non trouvée")
    })
    @PostMapping("/comments")
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> addComment(
            @Parameter(description = "UUID de la note") @PathVariable String noteUuid,
            @Valid @RequestBody CommentRequest commentRequest,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        log.info("Ajout commentaire sur note: {} par: {}", noteUuid, jwt.getSubject());
        CommentResponse response = noteCommentService.addComment(noteUuid, commentRequest, jwt);
        return ResponseEntity.ok(getResponse(request, Map.of("comment", response), "Commentaire ajouté avec succès", OK));
    }

    @Operation(summary = "Liste les commentaires", description = "Récupère tous les commentaires d'une note")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des commentaires"),
            @ApiResponse(responseCode = "404", description = "Note non trouvée")
    })
    @GetMapping("/comments")
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> getComments(
            @Parameter(description = "UUID de la note") @PathVariable String noteUuid,
            HttpServletRequest request
    ) {
        log.debug("Liste des commentaires pour note: {}", noteUuid);
        List<CommentResponse> comments = noteCommentService.getComments(noteUuid);
        return ResponseEntity.ok(getResponse(request, Map.of("comments", comments, "count", comments.size()),
                "Commentaires récupérés avec succès", OK));
    }

    @Operation(summary = "Modifie un commentaire", description = "Modifie un commentaire existant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commentaire modifié"),
            @ApiResponse(responseCode = "403", description = "Non autorisé"),
            @ApiResponse(responseCode = "404", description = "Commentaire non trouvé")
    })
    @PutMapping("/comments/{commentUuid}")
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> updateComment(
            @Parameter(description = "UUID de la note") @PathVariable String noteUuid,
            @Parameter(description = "UUID du commentaire") @PathVariable String commentUuid,
            @Valid @RequestBody CommentRequest commentRequest,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        log.info("Modification commentaire: {} sur note: {} par: {}", commentUuid, noteUuid, jwt.getSubject());
        CommentResponse response = noteCommentService.updateComment(noteUuid, commentUuid, commentRequest, jwt);
        return ResponseEntity.ok(getResponse(request, Map.of("comment", response), "Commentaire modifié avec succès", OK));
    }

    @Operation(summary = "Supprime un commentaire", description = "Supprime un commentaire d'une note")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commentaire supprimé"),
            @ApiResponse(responseCode = "403", description = "Non autorisé"),
            @ApiResponse(responseCode = "404", description = "Commentaire non trouvé")
    })
    @DeleteMapping("/comments/{commentUuid}")
    @PreAuthorize(ALL_STAFF)
    public ResponseEntity<Response> deleteComment(
            @Parameter(description = "UUID de la note") @PathVariable String noteUuid,
            @Parameter(description = "UUID du commentaire") @PathVariable String commentUuid,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        log.info("Suppression commentaire: {} de la note: {} par: {}", commentUuid, noteUuid, jwt.getSubject());
        noteCommentService.deleteComment(noteUuid, commentUuid, jwt);
        return ResponseEntity.ok(getResponse(request, Map.of(), "Commentaire supprimé avec succès", OK));
    }
}