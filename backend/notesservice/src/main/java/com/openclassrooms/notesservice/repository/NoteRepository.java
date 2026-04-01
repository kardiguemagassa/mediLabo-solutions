package com.openclassrooms.notesservice.repository;

import com.openclassrooms.notesservice.model.Note;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository MongoDB pour les notes
 *
 *  @author Kardigué MAGASSA
 *  @version 1.0
 *  @since 2026-02-02
 */

@Repository
public interface NoteRepository extends MongoRepository<Note, String> {

    List<Note> findByActiveTrueOrderByCreatedAtDesc();
    Optional<Note> findByNoteUuidAndActiveTrue(String noteUuid);
    List<Note> findByPatientUuidAndActiveTrueOrderByCreatedAtDesc(String patientUuid);
    List<Note> findByPractitionerUuidAndActiveTrueOrderByCreatedAtDesc(String practitionerUuid);
    boolean existsByNoteUuid(String noteUuid);
    long countByPatientUuidAndActiveTrue(String patientUuid);
}
