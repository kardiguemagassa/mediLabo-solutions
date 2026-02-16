package com.openclassrooms.notesservice.service;

import com.openclassrooms.notesservice.client.PatientServiceClient;
import com.openclassrooms.notesservice.config.FileStorageConfig;
import com.openclassrooms.notesservice.dto.FileResponse;
import com.openclassrooms.notesservice.dto.PatientInfo;
import com.openclassrooms.notesservice.enumeration.EventType;
import com.openclassrooms.notesservice.event.Event;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.model.FileAttachment;
import com.openclassrooms.notesservice.model.Note;
import com.openclassrooms.notesservice.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service pour la gestion des fichiers attachés aux notes.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteFileService {

    private final NoteRepository noteRepository;
    private final FileStorageService fileStorageService;
    private final FileStorageConfig fileStorageConfig;
    private final PatientServiceClient patientServiceClient;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Upload un fichier et l'attache à une note.
     *
     * @param noteUuid UUID de la note
     * @param file     Fichier à uploader
     * @param jwt      Token JWT de l'utilisateur
     * @return FileResponse avec les informations du fichier
     */
    public FileResponse uploadFile(String noteUuid, MultipartFile file, Jwt jwt) {
        Note note = noteRepository.findByNoteUuidAndActiveTrue(noteUuid)
                .orElseThrow(() -> new ApiException("Note non trouvée: " + noteUuid));

        // Stocker le fichier physiquement
        FileStorageService.StoredFileInfo storedInfo = fileStorageService.storeFile(noteUuid, file);

        // Créer l'attachment
        FileAttachment attachment = FileAttachment.builder()
                .fileUuid(storedInfo.getFileUuid())
                .originalName(storedInfo.getOriginalName())
                .storedName(storedInfo.getStoredName())
                .extension(storedInfo.getExtension())
                .contentType(storedInfo.getContentType())
                .size(storedInfo.getSize())
                .formattedSize(storedInfo.getFormattedSize())
                .uri(storedInfo.getRelativePath())
                .uploadedByUuid(jwt.getSubject())
                .uploadedByName(extractName(jwt))
                .uploadedByRole(extractRole(jwt))
                .uploadedAt(LocalDateTime.now())
                .build();

        // Ajouter à la note
        note.addFile(attachment);
        noteRepository.save(note);

        log.info("Fichier uploadé: {} pour la note: {}", storedInfo.getOriginalName(), noteUuid);

        // Publier l'événement Kafka avec les infos patient
        publishFileUploadedEvent(note, attachment);

        return mapToFileResponse(attachment, noteUuid);
    }

    /**
     * Liste tous les fichiers d'une note.
     *
     * @param noteUuid UUID de la note
     * @return Liste des fichiers
     */
    public List<FileResponse> getFiles(String noteUuid) {
        Note note = noteRepository.findByNoteUuidAndActiveTrue(noteUuid)
                .orElseThrow(() -> new ApiException("Note non trouvée: " + noteUuid));

        return note.getFiles().stream()
                .map(f -> mapToFileResponse(f, noteUuid))
                .toList();
    }

    /**
     * Télécharge un fichier.
     *
     * @param noteUuid UUID de la note
     * @param fileUuid UUID du fichier
     * @return FileDownload avec la ressource
     */
    public FileDownload downloadFile(String noteUuid, String fileUuid) {
        Note note = noteRepository.findByNoteUuidAndActiveTrue(noteUuid)
                .orElseThrow(() -> new ApiException("Note non trouvée: " + noteUuid));

        FileAttachment attachment = note.findFile(fileUuid);
        if (attachment == null) {
            throw new ApiException("Fichier non trouvé: " + fileUuid);
        }

        Resource resource = fileStorageService.loadFileAsResource(
                noteUuid,
                attachment.getFileUuid(),
                attachment.getExtension()
        );

        return FileDownload.builder()
                .resource(resource)
                .filename(attachment.getOriginalName())
                .contentType(attachment.getContentType())
                .build();
    }

    /**
     * Supprime un fichier d'une note.
     *
     * @param noteUuid UUID de la note
     * @param fileUuid UUID du fichier
     * @param jwt      Token JWT de l'utilisateur
     */
    public void deleteFile(String noteUuid, String fileUuid, Jwt jwt) {
        Note note = noteRepository.findByNoteUuidAndActiveTrue(noteUuid)
                .orElseThrow(() -> new ApiException("Note non trouvée: " + noteUuid));

        FileAttachment attachment = note.findFile(fileUuid);
        if (attachment == null) {
            throw new ApiException("Fichier non trouvé: " + fileUuid);
        }

        // Vérifier les droits (propriétaire du fichier ou praticien de la note)
        String userUuid = jwt.getSubject();
        if (!attachment.getUploadedByUuid().equals(userUuid)
                && !note.getPractitionerUuid().equals(userUuid)) {
            throw new ApiException("Non autorisé à supprimer ce fichier");
        }

        // Supprimer le fichier physique
        fileStorageService.deleteFile(noteUuid, fileUuid, attachment.getExtension());

        // Retirer de la note
        note.removeFile(fileUuid);
        noteRepository.save(note);

        log.info("Fichier supprimé: {} de la note: {}", fileUuid, noteUuid);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Publie un événement FILE_UPLOADED avec les infos patient.
     */
    private void publishFileUploadedEvent(Note note, FileAttachment file) {
        // APPEL ASYNC : On utilise .join() pour attendre le résultat car
        // cette méthode est appelée à la fin d'un processus d'upload (synchrone).
        // Ou mieux, on utilise getPatientContactInfoAsync que nous avons créé.

        patientServiceClient.getPatientContactInfoAsync(note.getPatientUuid())
                .thenAccept(patientOpt -> {
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
                    data.put("fileUuid", file.getFileUuid());
                    data.put("fileName", file.getOriginalName());
                    data.put("fileSize", file.getFormattedSize());
                    data.put("fileExtension", file.getExtension());

                    Event event = Event.builder()
                            .type(EventType.FILE_UPLOADED)
                            .noteUuid(note.getNoteUuid())
                            .patientUuid(note.getPatientUuid())
                            .patientEmail(patientEmail)
                            .patientName(patientName)
                            .practitionerUuid(file.getUploadedByUuid())
                            .practitionerName(file.getUploadedByName())
                            .practitionerRole(file.getUploadedByRole())
                            .data(data)
                            .build();

                    eventPublisher.publishEvent(event);
                    log.debug("FILE_UPLOADED event published for note: {}", note.getNoteUuid());
                })
                .exceptionally(ex -> {
                    log.error("Erreur lors de la publication de l'événement Kafka: {}", ex.getMessage());
                    return null;
                });
    }

    /**
     * Mappe un FileAttachment vers FileResponse.
     */
    private FileResponse mapToFileResponse(FileAttachment attachment, String noteUuid) {
        String downloadUrl = String.format("%s/%s/files/%s/download",
                fileStorageConfig.getBaseDownloadUrl(),
                noteUuid,
                attachment.getFileUuid()
        );

        return FileResponse.builder()
                .fileUuid(attachment.getFileUuid())
                .name(attachment.getOriginalName())
                .extension(attachment.getExtension())
                .contentType(attachment.getContentType())
                .size(attachment.getSize())
                .formattedSize(attachment.getFormattedSize())
                .downloadUrl(downloadUrl)
                .uploadedByUuid(attachment.getUploadedByUuid())
                .uploadedByName(attachment.getUploadedByName())
                .uploadedByRole(attachment.getUploadedByRole())
                .uploadedAt(attachment.getUploadedAt())
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
     * Wrapper pour le téléchargement de fichier.
     */
    @lombok.Data
    @lombok.Builder
    public static class FileDownload {
        private Resource resource;
        private String filename;
        private String contentType;
    }
}