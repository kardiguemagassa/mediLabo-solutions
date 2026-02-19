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

    /**
     * Stocke un fichier pour une note donnée.
     *
     * @param noteUuid UUID de la note
     * @param file     Fichier à stocker
     * @return Informations sur le fichier stocké
     */
    StoredFileInfo storeFile(String noteUuid, MultipartFile file);

    /**
     * Charge un fichier comme Resource.
     *
     * @param noteUuid  UUID de la note
     * @param fileUuid  UUID du fichier
     * @param extension Extension du fichier
     * @return Resource du fichier
     */
    Resource loadFileAsResource(String noteUuid, String fileUuid, String extension);

    /**
     * Supprime un fichier.
     *
     * @param noteUuid  UUID de la note
     * @param fileUuid  UUID du fichier
     * @param extension Extension du fichier
     * @return true si supprimé avec succès
     */
    boolean deleteFile(String noteUuid, String fileUuid, String extension);

    /**
     * Information sur un fichier stocké.
     */
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