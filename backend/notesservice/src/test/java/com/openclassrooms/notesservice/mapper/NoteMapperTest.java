package com.openclassrooms.notesservice.mapper;

import com.openclassrooms.notesservice.dto.*;
import com.openclassrooms.notesservice.model.Comment;
import com.openclassrooms.notesservice.model.FileAttachment;
import com.openclassrooms.notesservice.model.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NoteMapper Tests (MapStruct)")
class NoteMapperTest {

    private NoteMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(NoteMapper.class);
    }

    // toEntity
    @Nested
    @DisplayName("toEntity Tests")
    class ToEntityTests {

        @Test
        @DisplayName("Should convert NoteRequest to Note entity")
        void toEntity_validRequest_returnsNote() {
            NoteRequest request = NoteRequest.builder()
                    .patientUuid("patient-uuid-123")
                    .content("Observation médicale du patient")
                    .build();

            Note result = mapper.toEntity(request, "practitioner-uuid-456", "Dr. Dupont");

            assertThat(result).isNotNull();
            assertThat(result.getNoteUuid()).isNotNull().matches("[0-9a-f-]{36}");
            assertThat(result.getPatientUuid()).isEqualTo("patient-uuid-123");
            assertThat(result.getPractitionerUuid()).isEqualTo("practitioner-uuid-456");
            assertThat(result.getPractitionerName()).isEqualTo("Dr. Dupont");
            assertThat(result.getContent()).isEqualTo("Observation médicale du patient");
            assertThat(result.getActive()).isTrue();
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();
            assertThat(result.getId()).isNull(); // MongoDB génère l'ID
        }

        @Test
        @DisplayName("Should not set files or comments on new entity")
        void toEntity_newNote_noFilesOrComments() {
            NoteRequest request = NoteRequest.builder()
                    .patientUuid("patient-uuid")
                    .content("Content")
                    .build();

            Note result = mapper.toEntity(request, "pract-uuid", "Dr. Test");

            assertThat(result.getFiles()).isNullOrEmpty();
            assertThat(result.getComments()).isNullOrEmpty();
        }
    }

    // toResponse
    @Nested
    @DisplayName("toResponse Tests")
    class ToResponseTests {

        @Test
        @DisplayName("Should convert Note to NoteResponse with counts")
        void toResponse_noteWithFilesAndComments_returnsResponseWithCounts() {
            Note note = buildNote();
            note.setFiles(List.of(buildFileAttachment("file-1"), buildFileAttachment("file-2")));
            note.setComments(List.of(buildComment("comment-1")));

            NoteResponse result = mapper.toResponse(note);

            assertThat(result).isNotNull();
            assertThat(result.getNoteUuid()).isEqualTo("note-uuid-123");
            assertThat(result.getPatientUuid()).isEqualTo("patient-uuid");
            assertThat(result.getPractitionerUuid()).isEqualTo("pract-uuid");
            assertThat(result.getPractitionerName()).isEqualTo("Dr. Dupont");
            assertThat(result.getContent()).isEqualTo("Note content");
            assertThat(result.getFileCount()).isEqualTo(2);
            assertThat(result.getCommentCount()).isEqualTo(1);
            assertThat(result.getFiles()).hasSize(2);
            assertThat(result.getComments()).hasSize(1);
        }

        @Test
        @DisplayName("Should handle note with no files and no comments")
        void toResponse_emptyNote_zeroCountsAndEmptyLists() {
            Note note = buildNote();
            note.setFiles(List.of());
            note.setComments(List.of());

            NoteResponse result = mapper.toResponse(note);

            assertThat(result.getFileCount()).isZero();
            assertThat(result.getCommentCount()).isZero();
            assertThat(result.getFiles()).isEmpty();
            assertThat(result.getComments()).isEmpty();
        }

        @Test
        @DisplayName("Should return null when note is null")
        void toResponse_nullNote_returnsNull() {
            assertThat(mapper.toResponse(null)).isNull();
        }
    }

    // toResponseList
    @Nested
    @DisplayName("toResponseList Tests")
    class ToResponseListTests {

        @Test
        @DisplayName("Should convert list of notes")
        void toResponseList_validList_returnsResponses() {
            List<Note> notes = List.of(buildNote(), buildNote());

            List<NoteResponse> results = mapper.toResponseList(notes);

            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list for null")
        void toResponseList_null_returnsNull() {
            assertThat(mapper.toResponseList(null)).isNull();
        }

        @Test
        @DisplayName("Should return empty list for empty input")
        void toResponseList_empty_returnsEmpty() {
            assertThat(mapper.toResponseList(List.of())).isEmpty();
        }
    }

    // toComment
    @Nested
    @DisplayName("toComment Tests")
    class ToCommentTests {

        @Test
        @DisplayName("Should build Comment from request and JWT info")
        void toComment_validParams_returnsComment() {
            CommentRequest request = CommentRequest.builder()
                    .content("Commentaire important")
                    .build();

            Comment result = mapper.toComment(request, "author-uuid", "Dr. Martin", "PRACTITIONER", "http://img.com/avatar.jpg");

            assertThat(result).isNotNull();
            assertThat(result.getCommentUuid()).isNotNull().matches("[0-9a-f-]{36}");
            assertThat(result.getContent()).isEqualTo("Commentaire important");
            assertThat(result.getAuthorUuid()).isEqualTo("author-uuid");
            assertThat(result.getAuthorName()).isEqualTo("Dr. Martin");
            assertThat(result.getAuthorRole()).isEqualTo("PRACTITIONER");
            assertThat(result.getAuthorImageUrl()).isEqualTo("http://img.com/avatar.jpg");
            assertThat(result.getEdited()).isFalse();
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should handle null imageUrl")
        void toComment_nullImageUrl_handlesGracefully() {
            CommentRequest request = CommentRequest.builder().content("Test").build();

            Comment result = mapper.toComment(request, "uuid", "Name", "ROLE", null);

            assertThat(result).isNotNull();
            assertThat(result.getAuthorImageUrl()).isNull();
        }
    }

    // toCommentResponse
    @Nested
    @DisplayName("toCommentResponse Tests")
    class ToCommentResponseTests {

        @Test
        @DisplayName("Should convert Comment to CommentResponse")
        void toCommentResponse_validComment_returnsResponse() {
            Comment comment = buildComment("comment-uuid-1");

            CommentResponse result = mapper.toCommentResponse(comment);

            assertThat(result).isNotNull();
            assertThat(result.getCommentUuid()).isEqualTo("comment-uuid-1");
            assertThat(result.getContent()).isEqualTo("Comment content");
            assertThat(result.getAuthorUuid()).isEqualTo("author-uuid");
            assertThat(result.getAuthorName()).isEqualTo("Dr. Martin");
            assertThat(result.getAuthorRole()).isEqualTo("DOCTOR");
            assertThat(result.getEdited()).isFalse();
        }

        @Test
        @DisplayName("Should return null for null comment")
        void toCommentResponse_null_returnsNull() {
            assertThat(mapper.toCommentResponse(null)).isNull();
        }
    }

    // toFileResponse
    @Nested
    @DisplayName("toFileResponse Tests")
    class ToFileResponseTests {

        @Test
        @DisplayName("Should convert FileAttachment to FileResponse")
        void toFileResponse_validFile_returnsResponse() {
            FileAttachment file = buildFileAttachment("file-uuid-1");

            FileResponse result = mapper.toFileResponse(file);

            assertThat(result).isNotNull();
            assertThat(result.getFileUuid()).isEqualTo("file-uuid-1");
            assertThat(result.getName()).isEqualTo("document.pdf");
            assertThat(result.getExtension()).isEqualTo("pdf");
            assertThat(result.getContentType()).isEqualTo("application/pdf");
            assertThat(result.getSize()).isEqualTo(1024L);
            assertThat(result.getFormattedSize()).isEqualTo("1 KB");
            assertThat(result.getUploadedByName()).isEqualTo("Dr. Dupont");
            assertThat(result.getDownloadUrl()).isNull(); // set dans le service
        }

        @Test
        @DisplayName("Should return null for null file")
        void toFileResponse_null_returnsNull() {
            assertThat(mapper.toFileResponse(null)).isNull();
        }
    }

    // toCommentResponseList / toFileResponseList
    @Test
    @DisplayName("Should convert list of comments")
    void toCommentResponseList_validList_returnsResponses() {
        List<Comment> comments = List.of(buildComment("c1"), buildComment("c2"));
        List<CommentResponse> results = mapper.toCommentResponseList(comments);
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("Should convert list of files")
    void toFileResponseList_validList_returnsResponses() {
        List<FileAttachment> files = List.of(buildFileAttachment("f1"), buildFileAttachment("f2"));
        List<FileResponse> results = mapper.toFileResponseList(files);
        assertThat(results).hasSize(2);
    }

    // HELPERS
    private Note buildNote() {
        return Note.builder()
                .id("mongo-id")
                .noteUuid("note-uuid-123")
                .patientUuid("patient-uuid")
                .practitionerUuid("pract-uuid")
                .practitionerName("Dr. Dupont")
                .content("Note content")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .files(List.of())
                .comments(List.of())
                .build();
    }

    private Comment buildComment(String uuid) {
        return Comment.builder()
                .commentUuid(uuid)
                .content("Comment content")
                .authorUuid("author-uuid")
                .authorName("Dr. Martin")
                .authorRole("DOCTOR")
                .authorImageUrl("http://img.com/avatar.jpg")
                .edited(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private FileAttachment buildFileAttachment(String uuid) {
        return FileAttachment.builder()
                .fileUuid(uuid)
                .originalName("document.pdf")
                .storedName(uuid + ".pdf")
                .extension("pdf")
                .contentType("application/pdf")
                .size(1024L)
                .formattedSize("1 KB")
                .uri("notes/note-uuid/" + uuid + ".pdf")
                .uploadedByUuid("uploader-uuid")
                .uploadedByName("Dr. Dupont")
                .uploadedByRole("DOCTOR")
                .uploadedAt(LocalDateTime.now())
                .build();
    }
}