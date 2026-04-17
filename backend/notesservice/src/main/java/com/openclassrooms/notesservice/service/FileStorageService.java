package com.openclassrooms.notesservice.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service pour le stockage et la récupération de fichiers.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-07
 */
public interface FileStorageService {
    StoredFileInfo storeFile(String noteUuid, MultipartFile file);
    Resource loadFileAsResource(String noteUuid, String fileUuid, String extension);
    boolean deleteFile(String noteUuid, String fileUuid, String extension);

    @lombok.Data
    @lombok.Builder
    class StoredFileInfo {
        private String fileUuid;
        private String originalName;
        private String storedName;
        private String extension;
        private String contentType;
        private Long size;
        private String formattedSize;
        private String relativePath;
    }
}