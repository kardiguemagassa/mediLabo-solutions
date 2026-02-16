package com.openclassrooms.notesservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Configuration pour le stockage de fichiers.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-07
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.storage")
@Validated
public class FileStorageConfig {

    /**
     * Répertoire racine pour le stockage des fichiers.
     */
    @NotBlank
    private String uploadDir = "./uploads";

    /**
     * Taille maximale d'un fichier (en bytes).
     * Par défaut: 10 MB
     */
    private long maxFileSize = 10 * 1024 * 1024;

    /**
     * Taille maximale totale de la requête (en bytes).
     * Par défaut: 50 MB
     */
    private long maxRequestSize = 50 * 1024 * 1024;

    /**
     * Extensions de fichiers autorisées.
     */
    private List<String> allowedExtensions = List.of("pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png", "gif", "txt", "csv");

    /**
     * Types MIME autorisés.
     */
    private List<String> allowedContentTypes = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "image/jpeg",
            "image/png",
            "image/gif",
            "text/plain",
            "text/csv"
    );

    /**
     * URL de base pour le téléchargement des fichiers.
     */
    private String baseDownloadUrl = "/api/notes";
}