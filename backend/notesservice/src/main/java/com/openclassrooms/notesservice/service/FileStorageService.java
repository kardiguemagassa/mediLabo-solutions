package com.openclassrooms.notesservice.service;

import com.openclassrooms.notesservice.config.FileStorageConfig;
import com.openclassrooms.notesservice.exception.FileStorageException;
import com.openclassrooms.notesservice.exception.InvalidFileException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.UUID;

/**
 * Service pour le stockage et la récupération de fichiers.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageConfig config;
    private Path rootLocation;

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(config.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(this.rootLocation);
            log.info("Répertoire de stockage initialisé: {}", this.rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Impossible de créer le répertoire de stockage", e);
        }
    }

    /**
     * Stocke un fichier pour une note donnée.
     *
     * @param noteUuid UUID de la note
     * @param file     Fichier à stocker
     * @return Informations sur le fichier stocké
     */
    public StoredFileInfo storeFile(String noteUuid, MultipartFile file) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        String fileUuid = UUID.randomUUID().toString();
        String storedFilename = fileUuid + "." + extension;

        try {
            // Créer le sous-répertoire pour la note
            Path noteDir = this.rootLocation.resolve("notes").resolve(noteUuid);
            Files.createDirectories(noteDir);

            // Stocker le fichier
            Path targetLocation = noteDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("Fichier stocké: {} -> {}", originalFilename, targetLocation);

            return StoredFileInfo.builder()
                    .fileUuid(fileUuid)
                    .originalName(originalFilename)
                    .storedName(storedFilename)
                    .extension(extension)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .formattedSize(formatFileSize(file.getSize()))
                    .relativePath("notes/" + noteUuid + "/" + storedFilename)
                    .build();

        } catch (IOException e) {
            throw new FileStorageException("Impossible de stocker le fichier " + originalFilename, e);
        }
    }

    /**
     * Charge un fichier comme Resource.
     *
     * @param noteUuid  UUID de la note
     * @param fileUuid  UUID du fichier
     * @param extension Extension du fichier
     * @return Resource du fichier
     */
    public Resource loadFileAsResource(String noteUuid, String fileUuid, String extension) {
        try {
            String storedFilename = fileUuid + "." + extension;
            Path filePath = this.rootLocation
                    .resolve("notes")
                    .resolve(noteUuid)
                    .resolve(storedFilename)
                    .normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("Fichier non trouvé: " + fileUuid);
            }
        } catch (MalformedURLException e) {
            throw new FileStorageException("Fichier non trouvé: " + fileUuid, e);
        }
    }

    /**
     * Supprime un fichier.
     *
     * @param noteUuid  UUID de la note
     * @param fileUuid  UUID du fichier
     * @param extension Extension du fichier
     * @return true si supprimé avec succès
     */
    public boolean deleteFile(String noteUuid, String fileUuid, String extension) {
        try {
            String storedFilename = fileUuid + "." + extension;
            Path filePath = this.rootLocation
                    .resolve("notes")
                    .resolve(noteUuid)
                    .resolve(storedFilename)
                    .normalize();

            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Fichier supprimé: {}", filePath);
            }
            return deleted;
        } catch (IOException e) {
            log.error("Erreur lors de la suppression du fichier: {}", fileUuid, e);
            return false;
        }
    }

    /**
     * Valide un fichier avant stockage.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Le fichier est vide");
        }

        String filename = StringUtils.cleanPath(file.getOriginalFilename());

        if (filename.contains("..")) {
            throw new InvalidFileException("Nom de fichier invalide: " + filename);
        }

        // Vérifier la taille
        if (file.getSize() > config.getMaxFileSize()) {
            throw new InvalidFileException(
                    "Fichier trop volumineux. Taille max: " + formatFileSize(config.getMaxFileSize())
            );
        }

        // Vérifier l'extension
        String extension = getExtension(filename).toLowerCase();
        if (!config.getAllowedExtensions().contains(extension)) {
            throw new InvalidFileException(
                    "Extension non autorisée: " + extension + ". Extensions autorisées: " + config.getAllowedExtensions()
            );
        }

        // Vérifier le type MIME
        String contentType = file.getContentType();
        if (contentType != null && !config.getAllowedContentTypes().contains(contentType)) {
            throw new InvalidFileException(
                    "Type de fichier non autorisé: " + contentType
            );
        }
    }

    /**
     * Extrait l'extension d'un nom de fichier.
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * Formate la taille d'un fichier en format lisible.
     */
    public static String formatFileSize(long size) {
        if (size <= 0) return "0 B";

        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);

        return new DecimalFormat("#,##0.##")
                .format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    /**
     * Information sur un fichier stocké.
     */
    @lombok.Data
    @lombok.Builder
    public static class StoredFileInfo {
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