package com.openclassrooms.notesservice.service;

import com.openclassrooms.notesservice.dto.FileResponse;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service de gestion des fichiers attachés aux notes médicales.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-07
 */
public interface NoteFileService {

    /**
     * Upload un fichier et l'attache à une note.
     *
     * @param noteUuid UUID de la note
     * @param file     Fichier à uploader
     * @param jwt      Token JWT de l'utilisateur
     * @return FileResponse avec les informations du fichier
     */
    FileResponse uploadFile(String noteUuid, MultipartFile file, Jwt jwt);

    /**
     * Liste tous les fichiers d'une note.
     *
     * @param noteUuid UUID de la note
     * @return Liste des fichiers
     */
    List<FileResponse> getFiles(String noteUuid);

    /**
     * Télécharge un fichier.
     *
     * @param noteUuid UUID de la note
     * @param fileUuid UUID du fichier
     * @return FileDownload avec la ressource
     */
    FileDownload downloadFile(String noteUuid, String fileUuid);

    /**
     * Supprime un fichier d'une note.
     *
     * @param noteUuid UUID de la note
     * @param fileUuid UUID du fichier
     * @param jwt      Token JWT de l'utilisateur
     */
    void deleteFile(String noteUuid, String fileUuid, Jwt jwt);

    /**
     * Wrapper pour le téléchargement de fichier.
     */
    @lombok.Data
    @lombok.Builder
    class FileDownload {
        private Resource resource;
        private String filename;
        private String contentType;
    }
}