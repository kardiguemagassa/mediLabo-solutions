package com.openclassrooms.notesservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Note - Document MongoDB
 * Représente une note d'observation médicale avec fichiers et commentaires embedded.
 * NOTE: Les index sont gérés manuellement via le script db
 * Ne pas utiliser @Indexed ou @CompoundIndex les conflits.
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-02
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notes")
public class Note {

    @Id
    private String id;

    /**
     * UUID unique de la note.
     * Index: idx_note_uuid_unique (unique) - géré par script MongoDB
     */
    private String noteUuid;

    /**
     * UUID du patient.
     * Index: idx_patient_created_at, idx_patient_active_created - géré par script MongoDB
     */
    private String patientUuid;

    /**
     * UUID du praticien.
     * Index: idx_practitioner_uuid - géré par script MongoDB
     */
    private String practitionerUuid;
    private String practitionerName;
    private String content;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<FileAttachment> files = new ArrayList<>();

    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    public void addFile(FileAttachment file) {
        if (this.files == null) {
            this.files = new ArrayList<>();
        }
        this.files.add(file);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean removeFile(String fileUuid) {
        if (this.files == null) return false;
        boolean removed = this.files.removeIf(f -> f.getFileUuid().equals(fileUuid));
        if (removed) {
            this.updatedAt = LocalDateTime.now();
        }
        return removed;
    }

    public FileAttachment findFile(String fileUuid) {
        if (this.files == null) return null;
        return this.files.stream()
                .filter(f -> f.getFileUuid().equals(fileUuid))
                .findFirst()
                .orElse(null);
    }

    public int getFileCount() {
        return this.files == null ? 0 : this.files.size();
    }

    public void addComment(Comment comment) {
        if (this.comments == null) {
            this.comments = new ArrayList<>();
        }
        this.comments.add(comment);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean removeComment(String commentUuid) {
        if (this.comments == null) return false;
        boolean removed = this.comments.removeIf(c -> c.getCommentUuid().equals(commentUuid));
        if (removed) {
            this.updatedAt = LocalDateTime.now();
        }
        return removed;
    }

    public Comment findComment(String commentUuid) {
        if (this.comments == null) return null;
        return this.comments.stream()
                .filter(c -> c.getCommentUuid().equals(commentUuid))
                .findFirst()
                .orElse(null);
    }

    public int getCommentCount() {
        return this.comments == null ? 0 : this.comments.size();
    }
}