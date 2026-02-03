package com.openclassrooms.notesservice.repository;

import com.openclassrooms.notesservice.model.Note;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("NoteRepository Integration Tests")
class NoteRepositoryIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "test");
    }

    @Autowired
    private NoteRepository noteRepository;

    private Note testNote;
    private String patientUuid;
    private String practitionerUuid;

    @BeforeEach
    void setUp() {
        noteRepository.deleteAll();

        patientUuid = UUID.randomUUID().toString();
        practitionerUuid = UUID.randomUUID().toString();

        testNote = Note.builder()
                .noteUuid(UUID.randomUUID().toString())
                .patientUuid(patientUuid)
                .practitionerUuid(practitionerUuid)
                .practitionerName("Dr. Test")
                .content("Test note content")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        noteRepository.deleteAll();
    }

    @Nested
    @DisplayName("Save Note Tests")
    class SaveNoteTests {

        @Test
        @DisplayName("Should save note successfully")
        void save_validNote_returnsSavedNote() {
            // When
            Note savedNote = noteRepository.save(testNote);

            // Then
            assertThat(savedNote).isNotNull();
            assertThat(savedNote.getId()).isNotNull();
            assertThat(savedNote.getNoteUuid()).isEqualTo(testNote.getNoteUuid());
            assertThat(savedNote.getContent()).isEqualTo("Test note content");
            assertThat(savedNote.getActive()).isTrue();  // Changé ici
        }

        @Test
        @DisplayName("Should update existing note")
        void save_existingNote_updatesNote() {
            // Given
            Note savedNote = noteRepository.save(testNote);
            savedNote.setContent("Updated content");
            savedNote.setUpdatedAt(LocalDateTime.now());

            // When
            Note updatedNote = noteRepository.save(savedNote);

            // Then
            assertThat(updatedNote.getContent()).isEqualTo("Updated content");
            assertThat(updatedNote.getId()).isEqualTo(savedNote.getId());
        }
    }

    @Nested
    @DisplayName("Find By NoteUuid And Active Tests")
    class FindByNoteUuidAndActiveTrueTests {

        @Test
        @DisplayName("Should find note by UUID when exists and active")
        void findByNoteUuidAndActiveTrue_existingActiveNote_returnsNote() {
            // Given
            noteRepository.save(testNote);

            // When
            Optional<Note> found = noteRepository.findByNoteUuidAndActiveTrue(testNote.getNoteUuid());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getNoteUuid()).isEqualTo(testNote.getNoteUuid());
            assertThat(found.get().getActive()).isTrue();  // Changé ici
        }

        @Test
        @DisplayName("Should return empty when note not found")
        void findByNoteUuidAndActiveTrue_nonExistingNote_returnsEmpty() {
            // When
            Optional<Note> found = noteRepository.findByNoteUuidAndActiveTrue("non-existing-uuid");

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should not find inactive note")
        void findByNoteUuidAndActiveTrue_inactiveNote_returnsEmpty() {
            // Given
            testNote.setActive(false);
            noteRepository.save(testNote);

            // When
            Optional<Note> found = noteRepository.findByNoteUuidAndActiveTrue(testNote.getNoteUuid());

            // Then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find By Patient UUID Tests")
    class FindByPatientUuidTests {

        @Test
        @DisplayName("Should find all active notes for patient ordered by creation date desc")
        void findByPatientUuidAndActiveTrueOrderByCreatedAtDesc_existingNotes_returnsSortedList() {
            // Given
            testNote.setCreatedAt(LocalDateTime.now().minusDays(1));
            noteRepository.save(testNote);

            Note newerNote = Note.builder()
                    .noteUuid(UUID.randomUUID().toString())
                    .patientUuid(patientUuid)
                    .practitionerUuid(practitionerUuid)
                    .practitionerName("Dr. Test")
                    .content("Newer note")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            noteRepository.save(newerNote);

            // When
            List<Note> notes = noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc(patientUuid);

            // Then
            assertThat(notes).hasSize(2);
            assertThat(notes.get(0).getContent()).isEqualTo("Newer note");
            assertThat(notes.get(1).getContent()).isEqualTo("Test note content");
        }

        @Test
        @DisplayName("Should return empty list when no notes for patient")
        void findByPatientUuidAndActiveTrueOrderByCreatedAtDesc_noNotes_returnsEmptyList() {
            // When
            List<Note> notes = noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc("unknown-patient");

            // Then
            assertThat(notes).isEmpty();
        }

        @Test
        @DisplayName("Should not include inactive notes")
        void findByPatientUuidAndActiveTrueOrderByCreatedAtDesc_withInactiveNotes_excludesInactive() {
            // Given
            noteRepository.save(testNote);

            Note inactiveNote = Note.builder()
                    .noteUuid(UUID.randomUUID().toString())
                    .patientUuid(patientUuid)
                    .practitionerUuid(practitionerUuid)
                    .practitionerName("Dr. Test")
                    .content("Inactive note")
                    .active(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            noteRepository.save(inactiveNote);

            // When
            List<Note> notes = noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc(patientUuid);

            // Then
            assertThat(notes).hasSize(1);
            assertThat(notes.get(0).getNoteUuid()).isEqualTo(testNote.getNoteUuid());
        }
    }

    @Nested
    @DisplayName("Find By Practitioner UUID Tests")
    class FindByPractitionerUuidTests {

        @Test
        @DisplayName("Should find all active notes by practitioner ordered by creation date desc")
        void findByPractitionerUuidAndActiveTrueOrderByCreatedAtDesc_existingNotes_returnsSortedList() {
            // Given
            testNote.setCreatedAt(LocalDateTime.now().minusDays(1));
            noteRepository.save(testNote);

            Note newerNote = Note.builder()
                    .noteUuid(UUID.randomUUID().toString())
                    .patientUuid(UUID.randomUUID().toString())
                    .practitionerUuid(practitionerUuid)
                    .practitionerName("Dr. Test")
                    .content("Newer note from same practitioner")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            noteRepository.save(newerNote);

            // When
            List<Note> notes = noteRepository.findByPractitionerUuidAndActiveTrueOrderByCreatedAtDesc(practitionerUuid);

            // Then
            assertThat(notes).hasSize(2);
            assertThat(notes.get(0).getContent()).isEqualTo("Newer note from same practitioner");
        }

        @Test
        @DisplayName("Should return empty list when practitioner has no notes")
        void findByPractitionerUuidAndActiveTrueOrderByCreatedAtDesc_noNotes_returnsEmptyList() {
            // When
            List<Note> notes = noteRepository.findByPractitionerUuidAndActiveTrueOrderByCreatedAtDesc("unknown-practitioner");

            // Then
            assertThat(notes).isEmpty();
        }

        @Test
        @DisplayName("Should not include inactive notes")
        void findByPractitionerUuidAndActiveTrueOrderByCreatedAtDesc_withInactiveNotes_excludesInactive() {
            // Given
            noteRepository.save(testNote);

            Note inactiveNote = Note.builder()
                    .noteUuid(UUID.randomUUID().toString())
                    .patientUuid(patientUuid)
                    .practitionerUuid(practitionerUuid)
                    .practitionerName("Dr. Test")
                    .content("Inactive note")
                    .active(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            noteRepository.save(inactiveNote);

            // When
            List<Note> notes = noteRepository.findByPractitionerUuidAndActiveTrueOrderByCreatedAtDesc(practitionerUuid);

            // Then
            assertThat(notes).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Exists By NoteUuid Tests")
    class ExistsByNoteUuidTests {

        @Test
        @DisplayName("Should return true when note exists")
        void existsByNoteUuid_existingNote_returnsTrue() {
            // Given
            noteRepository.save(testNote);

            // When
            boolean exists = noteRepository.existsByNoteUuid(testNote.getNoteUuid());

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when note does not exist")
        void existsByNoteUuid_nonExistingNote_returnsFalse() {
            // When
            boolean exists = noteRepository.existsByNoteUuid("non-existing-uuid");

            // Then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return true even for inactive note")
        void existsByNoteUuid_inactiveNote_returnsTrue() {
            // Given
            testNote.setActive(false);
            noteRepository.save(testNote);

            // When
            boolean exists = noteRepository.existsByNoteUuid(testNote.getNoteUuid());

            // Then
            assertThat(exists).isTrue();
        }
    }

    @Nested
    @DisplayName("Count By Patient UUID Tests")
    class CountByPatientUuidTests {

        @Test
        @DisplayName("Should count active notes for patient")
        void countByPatientUuidAndActiveTrue_existingNotes_returnsCount() {
            // Given
            noteRepository.save(testNote);

            Note secondNote = Note.builder()
                    .noteUuid(UUID.randomUUID().toString())
                    .patientUuid(patientUuid)
                    .practitionerUuid(practitionerUuid)
                    .practitionerName("Dr. Test")
                    .content("Second note")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            noteRepository.save(secondNote);

            // When
            long count = noteRepository.countByPatientUuidAndActiveTrue(patientUuid);

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return zero when no notes for patient")
        void countByPatientUuidAndActiveTrue_noNotes_returnsZero() {
            // When
            long count = noteRepository.countByPatientUuidAndActiveTrue("unknown-patient");

            // Then
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Should not count inactive notes")
        void countByPatientUuidAndActiveTrue_withInactiveNotes_excludesInactive() {
            // Given
            noteRepository.save(testNote);

            Note inactiveNote = Note.builder()
                    .noteUuid(UUID.randomUUID().toString())
                    .patientUuid(patientUuid)
                    .practitionerUuid(practitionerUuid)
                    .practitionerName("Dr. Test")
                    .content("Inactive note")
                    .active(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            noteRepository.save(inactiveNote);

            // When
            long count = noteRepository.countByPatientUuidAndActiveTrue(patientUuid);

            // Then
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Delete Note Tests")
    class DeleteNoteTests {

        @Test
        @DisplayName("Should delete note from database")
        void delete_existingNote_removesFromDatabase() {
            // Given
            Note savedNote = noteRepository.save(testNote);

            // When
            noteRepository.delete(savedNote);

            // Then
            Optional<Note> found = noteRepository.findById(savedNote.getId());
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should delete all notes")
        void deleteAll_multipleNotes_removesAll() {
            // Given
            noteRepository.save(testNote);
            Note secondNote = Note.builder()
                    .noteUuid(UUID.randomUUID().toString())
                    .patientUuid(patientUuid)
                    .practitionerUuid(practitionerUuid)
                    .practitionerName("Dr. Test")
                    .content("Second note")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            noteRepository.save(secondNote);

            // When
            noteRepository.deleteAll();

            // Then
            assertThat(noteRepository.count()).isZero();
        }
    }
}