package com.openclassrooms.notesservice.service.implementation;

import com.openclassrooms.notesservice.config.FileStorageConfig;
import com.openclassrooms.notesservice.dto.FileResponse;
import com.openclassrooms.notesservice.dto.PatientInfo;
import com.openclassrooms.notesservice.enumeration.EventType;
import com.openclassrooms.notesservice.event.Event;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.model.FileAttachment;
import com.openclassrooms.notesservice.model.Note;
import com.openclassrooms.notesservice.repository.NoteRepository;
import com.openclassrooms.notesservice.service.FileStorageService;
import com.openclassrooms.notesservice.service.NoteFileService;
import com.openclassrooms.notesservice.service.PatientServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Implémentation réactive du service de gestion des fichiers attachés aux notes.
 * ARCHITECTURE RÉACTIVE:
 * Mono.fromCallable() : Encapsule les appels bloquants (MongoDB, FileSystem)
 * subscribeOn(Schedulers.boundedElastic()) : Exécute sur thread-pool élastique
 * Publication d'événements Kafka de manière réactive
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteFileServiceImpl implements NoteFileService {

    private final NoteRepository noteRepository;
    private final FileStorageService fileStorageService;
    private final FileStorageConfig fileStorageConfig;
    private final PatientServiceClient patientServiceClient;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Upload un fichier et l'attache à une note.
     * 1. Récupération de la note
     * 2. Stockage du fichier physiquement
     * 3. Création de l'attachment
     * 4. Ajout à la note et sauvegarde MongoDB
     * 5. Publication de l'événement Kafka (async)
     * 6. Retour du FileResponse
     */
    @Override
    public Mono<FileResponse> uploadFile(String noteUuid, MultipartFile file, Jwt jwt) {
        log.debug("Uploading file to note: {}", noteUuid);

        return findNoteByUuid(noteUuid)
                .flatMap(note -> Mono.fromCallable(() -> {
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

                            //Retourner un tuple (note, attachment) pour l'événement
                            return new NoteFileContext(note, attachment);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        //Publier l'événement de manière réactive (fire and forget)
                        .doOnSuccess(context -> publishFileUploadedEvent(context.note(), context.attachment()))
                        .map(context -> mapToFileResponse(context.attachment(), noteUuid)));
    }

    /**
     * Liste tous les fichiers d'une note.
     */
    @Override
    public Flux<FileResponse> getFiles(String noteUuid) {
        log.debug("Getting files for note: {}", noteUuid);

        return findNoteByUuid(noteUuid)
                .flatMapMany(note -> Flux.fromIterable(note.getFiles()))
                .map(file -> mapToFileResponse(file, noteUuid));
    }

    /**
     * Télécharge un fichier.
     */
    @Override
    public Mono<FileDownload> downloadFile(String noteUuid, String fileUuid) {
        log.debug("Downloading file: {} from note: {}", fileUuid, noteUuid);

        return findNoteByUuid(noteUuid)
                .flatMap(note -> {
                    FileAttachment attachment = note.findFile(fileUuid);
                    if (attachment == null) {
                        return Mono.error(new ApiException("Fichier non trouvé: " + fileUuid));
                    }

                    // Charger le fichier depuis le filesystem
                    return Mono.fromCallable(() -> {
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
                            })
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }

    /**
     * Supprime un fichier d'une note.
     * FLUX:
     * Récupération de la note
     * Recherche du fichier
     * Vérification des droits (propriétaire ou praticien)
     * Suppression physique
     * Retrait de la note et sauvegarde
     */
    @Override
    public Mono<Void> deleteFile(String noteUuid, String fileUuid, Jwt jwt) {
        log.debug("Deleting file: {} from note: {}", fileUuid, noteUuid);

        return findNoteByUuid(noteUuid)
                .flatMap(note -> {
                    FileAttachment attachment = note.findFile(fileUuid);
                    if (attachment == null) {
                        return Mono.error(new ApiException("Fichier non trouvé: " + fileUuid));
                    }

                    // Vérifier les droits
                    String userUuid = jwt.getSubject();
                    if (!attachment.getUploadedByUuid().equals(userUuid)
                            && !note.getPractitionerUuid().equals(userUuid)) {
                        return Mono.error(new ApiException("Non autorisé à supprimer ce fichier"));
                    }

                    return Mono.fromCallable(() -> {
                                // Supprimer le fichier physique
                                fileStorageService.deleteFile(noteUuid, fileUuid, attachment.getExtension());

                                // Retirer de la note
                                note.removeFile(fileUuid);
                                noteRepository.save(note);

                                log.info("Fichier supprimé: {} de la note: {}", fileUuid, noteUuid);
                                return note;
                            })
                            .doOnSuccess(n -> publishFileDeletedEvent(n, attachment))
                            .subscribeOn(Schedulers.boundedElastic());
                })
                .then();
    }

    private void publishFileDeletedEvent(Note note, FileAttachment file) {
        patientServiceClient.getPatientContactInfo(note.getPatientUuid())
                .subscribe(
                        patient -> {
                            Map<String, Object> data = new HashMap<>();
                            data.put("name", patient != null ? patient.getFullName() : null);
                            data.put("email", patient != null ? patient.getEmail() : null);
                            data.put("recordNumber", note.getNoteUuid());
                            data.put("subject", "Fichier supprimé");
                            data.put("uploaderName", file.getUploadedByName());
                            data.put("date", LocalDateTime.now().toString());
                            data.put("files", file.getOriginalName());

                            Event event = Event.builder()
                                    .eventType(EventType.FILE_DELETED)
                                    .data(data)
                                    .build();
                            eventPublisher.publishEvent(event);
                            log.debug("FILE_DELETED event published for note: {}", note.getNoteUuid());
                        },
                        error -> log.error("Erreur lors de la récupération patient pour event: {}", error.getMessage())
                );
    }

    /**
     * Recherche une note par UUID.
     */
    private Mono<Note> findNoteByUuid(String noteUuid) {
        return Mono.fromCallable(() -> noteRepository.findByNoteUuidAndActiveTrue(noteUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new ApiException("Note non trouvée: " + noteUuid))));
    }

    /**
     * Publie un événement FILE_UPLOADED avec les infos patient.
     * Exécution asynchrone (fire and forget).
     */
    private void publishFileUploadedEvent(Note note, FileAttachment file) {
        patientServiceClient.getPatientContactInfo(note.getPatientUuid())
                .subscribe(
                        patient -> {
                            Map<String, Object> data = buildEventData(note, file, patient);
                            Event event = Event.builder()
                                    .eventType(EventType.FILE_UPLOADED)
                                    .data(data)
                                    .build();
                            eventPublisher.publishEvent(event);
                            log.debug("FILE_UPLOADED event published for note: {}", note.getNoteUuid());
                        },
                        error -> log.error("Erreur lors de la récupération patient pour event: {}", error.getMessage()),
                        () -> log.debug("No patient info found for event, skipping notification")
                );
    }

    /**
     * Construit les données de l'événement.
     */
    private Map<String, Object> buildEventData(Note note, FileAttachment file, PatientInfo patient) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", patient != null ? patient.getFullName() : null);
        data.put("email", patient != null ? patient.getEmail() : null);
        data.put("recordNumber", note.getNoteUuid());
        data.put("subject", "Note médicale - " + note.getPatientUuid());
        data.put("uploaderName", file.getUploadedByName());
        data.put("date", LocalDateTime.now().toString());
        data.put("files", file.getOriginalName());
        return data;
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
     * Record interne pour transporter note et attachment ensemble.
     */
    private record NoteFileContext(Note note, FileAttachment attachment) {}
}