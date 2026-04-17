package com.openclassrooms.notesservice.service;

import com.openclassrooms.notesservice.dto.FileResponse;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service de gestion des fichiers attachés aux notes médicales (Full Réactif).
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
public interface NoteFileService {
    Mono<FileResponse> uploadFile(String noteUuid, MultipartFile file, Jwt jwt);
    Flux<FileResponse> getFiles(String noteUuid);
    Mono<FileDownload> downloadFile(String noteUuid, String fileUuid);
    Mono<Void> deleteFile(String noteUuid, String fileUuid, Jwt jwt);

    @lombok.Data
    @lombok.Builder
    class FileDownload {
        private Resource resource;
        private String filename;
        private String contentType;
    }
}