package com.openclassrooms.notesservice.service.implementation;

import com.openclassrooms.notesservice.dto.NoteRequest;
import com.openclassrooms.notesservice.dto.NoteResponse;
import com.openclassrooms.notesservice.exception.ApiException;
import com.openclassrooms.notesservice.mapper.NoteMapper;
import com.openclassrooms.notesservice.model.Note;
import com.openclassrooms.notesservice.repository.NoteRepository;
import com.openclassrooms.notesservice.service.PatientServiceClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoteServiceImpl Tests")
class NoteServiceImplTest {

    @Mock private NoteRepository noteRepository;
    @Mock private NoteMapper noteMapper;
    @Mock private PatientServiceClient patientServiceClient;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NoteServiceImpl noteService;

    private Note note;
    private NoteResponse noteResponse;
    private NoteRequest noteRequest;

    @BeforeEach
    void setUp() {
        note = Note.builder()
                .id("mongo-id")
                .noteUuid("note-uuid-123")
                .patientUuid("patient-uuid-456")
                .practitionerUuid("pract-uuid-789")
                .practitionerName("Dr. Dupont")
                .content("Observation médicale")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .files(List.of())
                .comments(List.of())
                .build();

        noteResponse = NoteResponse.builder()
                .noteUuid("note-uuid-123")
                .patientUuid("patient-uuid-456")
                .practitionerName("Dr. Dupont")
                .content("Observation médicale")
                .fileCount(0)
                .commentCount(0)
                .build();

        noteRequest = NoteRequest.builder()
                .patientUuid("patient-uuid-456")
                .content("Observation médicale")
                .build();
    }

    @Nested
    @DisplayName("getAllActiveNotes Tests")
    class GetAllActiveNotesTests {

        @Test
        @DisplayName("Should return all active notes")
        void shouldReturnAllActiveNotes() {
            when(noteRepository.findByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(note));
            when(noteMapper.toResponse(any(Note.class))).thenReturn(noteResponse);

            StepVerifier.create(noteService.getAllActiveNotes()).expectNext(noteResponse).verifyComplete();
        }

        @Test
        @DisplayName("Should return empty when no notes")
        void shouldReturnEmpty_noNotes() {
            when(noteRepository.findByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of());
            StepVerifier.create(noteService.getAllActiveNotes()).expectNextCount(0).verifyComplete();
        }
    }

    @Nested
    @DisplayName("getAllActiveNotesPageable Tests")
    class GetAllActiveNotesPageableTests {

        @Test
        @DisplayName("Should return paginated notes")
        void shouldReturnPagedNotes() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            Page<Note> page = new PageImpl<>(List.of(note), pageable, 1);

            when(noteRepository.findByActiveTrueOrderByCreatedAtDesc(pageable)).thenReturn(page);
            when(noteMapper.toResponse(any(Note.class))).thenReturn(noteResponse);

            StepVerifier.create(noteService.getAllActiveNotesPageable(pageable))
                    .expectNextMatches(p -> p.getTotalElements() == 1
                            && p.getContent().getFirst().getNoteUuid().equals("note-uuid-123"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty page when no notes")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Note> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(noteRepository.findByActiveTrueOrderByCreatedAtDesc(pageable)).thenReturn(emptyPage);

            StepVerifier.create(noteService.getAllActiveNotesPageable(pageable))
                    .expectNextMatches(p -> p.getTotalElements() == 0 && p.getContent().isEmpty())
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("createNote Tests")
    class CreateNoteTests {

        @Test
        @DisplayName("Should create note successfully")
        void shouldCreateNote() {
            when(noteMapper.toEntity(any(), anyString(), anyString())).thenReturn(note);
            when(noteRepository.save(any(Note.class))).thenReturn(note);
            when(noteMapper.toResponse(any(Note.class))).thenReturn(noteResponse);
            when(patientServiceClient.getPatientContactInfo(anyString())).thenReturn(reactor.core.publisher.Mono.empty());

            StepVerifier.create(noteService.createNote(noteRequest, "pract-uuid-789", "Dr. Dupont"))
                    .expectNextMatches(r -> r.getNoteUuid().equals("note-uuid-123"))
                    .verifyComplete();

            verify(noteRepository).save(any(Note.class));
        }
    }

    @Nested
    @DisplayName("getNoteByUuid Tests")
    class GetNoteByUuidTests {

        @Test
        @DisplayName("Should return note by UUID")
        void shouldReturnNote() {
            when(noteRepository.findByNoteUuidAndActiveTrue("note-uuid-123")).thenReturn(Optional.of(note));
            when(noteMapper.toResponse(note)).thenReturn(noteResponse);

            StepVerifier.create(noteService.getNoteByUuid("note-uuid-123")).expectNext(noteResponse).verifyComplete();
        }

        @Test
        @DisplayName("Should fail when note not found")
        void shouldFail_noteNotFound() {
            when(noteRepository.findByNoteUuidAndActiveTrue("unknown")).thenReturn(Optional.empty());

            StepVerifier.create(noteService.getNoteByUuid("unknown"))
                    .expectErrorMatches(e -> e instanceof ApiException && e.getMessage().contains("Note non trouvée"))
                    .verify();
        }
    }

    @Nested
    @DisplayName("getNotesByPatientUuid Tests")
    class GetNotesByPatientUuidTests {

        @Test
        @DisplayName("Should return notes for patient")
        void shouldReturnNotes() {
            when(noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc("patient-uuid-456"))
                    .thenReturn(List.of(note));
            when(noteMapper.toResponse(any(Note.class))).thenReturn(noteResponse);

            StepVerifier.create(noteService.getNotesByPatientUuid("patient-uuid-456")).expectNext(noteResponse).verifyComplete();
        }
    }

    @Nested
    @DisplayName("getNotesByPatientUuidPageable Tests")
    class GetNotesByPatientUuidPageableTests {

        @Test
        @DisplayName("Should return paginated notes for patient")
        void shouldReturnPagedNotes() {
            Pageable pageable = PageRequest.of(0, 5);
            Page<Note> page = new PageImpl<>(List.of(note), pageable, 1);

            when(noteRepository.findByPatientUuidAndActiveTrueOrderByCreatedAtDesc("patient-uuid-456", pageable))
                    .thenReturn(page);
            when(noteMapper.toResponse(any(Note.class))).thenReturn(noteResponse);

            StepVerifier.create(noteService.getNotesByPatientUuidPageable("patient-uuid-456", pageable))
                    .expectNextMatches(p -> p.getTotalElements() == 1)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("updateNote Tests")
    class UpdateNoteTests {

        @Test
        @DisplayName("Should update note successfully when author")
        void shouldUpdateNote_whenAuthor() {
            when(noteRepository.findByNoteUuidAndActiveTrue("note-uuid-123")).thenReturn(Optional.of(note));
            when(noteRepository.save(any(Note.class))).thenReturn(note);
            when(noteMapper.toResponse(any(Note.class))).thenReturn(noteResponse);
            when(patientServiceClient.getPatientContactInfo(anyString())).thenReturn(reactor.core.publisher.Mono.empty());

            NoteRequest updateRequest = NoteRequest.builder().content("Nouveau contenu").build();

            StepVerifier.create(noteService.updateNote("note-uuid-123", updateRequest, "pract-uuid-789", false))
                    .expectNext(noteResponse)
                    .verifyComplete();

            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("Should allow SUPER_ADMIN to update any note")
        void shouldUpdateNote_asSuperAdmin() {
            when(noteRepository.findByNoteUuidAndActiveTrue("note-uuid-123")).thenReturn(Optional.of(note));
            when(noteRepository.save(any(Note.class))).thenReturn(note);
            when(noteMapper.toResponse(any(Note.class))).thenReturn(noteResponse);
            when(patientServiceClient.getPatientContactInfo(anyString())).thenReturn(reactor.core.publisher.Mono.empty());

            NoteRequest updateRequest = NoteRequest.builder().content("Admin override").build();

            // UUID différent de l'auteur mais isSuperAdmin = true
            StepVerifier.create(noteService.updateNote("note-uuid-123", updateRequest, "other-uuid", true))
                    .expectNext(noteResponse)
                    .verifyComplete();

            verify(noteRepository).save(any(Note.class));
        }

        @Test
        @DisplayName("Should fail when not author and not SUPER_ADMIN")
        void shouldFail_notAuthor() {
            when(noteRepository.findByNoteUuidAndActiveTrue("note-uuid-123")).thenReturn(Optional.of(note));

            NoteRequest updateRequest = NoteRequest.builder().content("Hijack attempt").build();

            StepVerifier.create(noteService.updateNote("note-uuid-123", updateRequest, "other-uuid", false))
                    .expectErrorMatches(e -> e instanceof ApiException
                            && e.getMessage().contains("Non autorisé"))
                    .verify();

            verify(noteRepository, never()).save(any(Note.class));
        }

        @Test
        @DisplayName("Should fail when note not found for update")
        void shouldFail_noteNotFound() {
            when(noteRepository.findByNoteUuidAndActiveTrue("unknown")).thenReturn(Optional.empty());

            StepVerifier.create(noteService.updateNote("unknown", noteRequest, "pract-uuid", false))
                    .expectErrorMatches(e -> e instanceof ApiException)
                    .verify();
        }
    }

    @Nested
    @DisplayName("deleteNote Tests")
    class DeleteNoteTests {

        @Test
        @DisplayName("Should soft delete note")
        void shouldSoftDelete() {
            when(noteRepository.findByNoteUuidAndActiveTrue("note-uuid-123")).thenReturn(Optional.of(note));
            when(noteRepository.save(any(Note.class))).thenReturn(note);

            StepVerifier.create(noteService.deleteNote("note-uuid-123")).verifyComplete();
            verify(noteRepository).save(argThat(n -> !n.getActive()));
        }

        @Test
        @DisplayName("Should fail when note not found for delete")
        void shouldFail_noteNotFound() {
            when(noteRepository.findByNoteUuidAndActiveTrue("unknown")).thenReturn(Optional.empty());

            StepVerifier.create(noteService.deleteNote("unknown")).expectErrorMatches(e -> e instanceof ApiException).verify();
        }
    }

    @Nested
    @DisplayName("countNotesByPatientUuid Tests")
    class CountNotesTests {

        @Test
        @DisplayName("Should return count")
        void shouldReturnCount() {
            when(noteRepository.countByPatientUuidAndActiveTrue("patient-uuid-456")).thenReturn(5L);

            StepVerifier.create(noteService.countNotesByPatientUuid("patient-uuid-456")).expectNext(5L).verifyComplete();
        }
    }
}