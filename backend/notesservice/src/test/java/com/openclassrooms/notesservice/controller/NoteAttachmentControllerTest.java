package com.openclassrooms.notesservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.notesservice.dto.CommentRequest;
import com.openclassrooms.notesservice.dto.CommentResponse;
import com.openclassrooms.notesservice.dto.FileResponse;
import com.openclassrooms.notesservice.service.NoteCommentService;
import com.openclassrooms.notesservice.service.NoteFileService;
import com.openclassrooms.notesservice.service.NoteFileService.FileDownload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour NoteAttachmentController (réactif avec Mono/Flux)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NoteAttachmentController Unit Tests")
class NoteAttachmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NoteFileService noteFileService;

    @Mock
    private NoteCommentService noteCommentService;

    @InjectMocks
    private NoteAttachmentController noteAttachmentController;

    private ObjectMapper objectMapper;
    private Jwt mockJwt;
    private FileResponse testFileResponse;
    private CommentResponse testCommentResponse;
    private CommentRequest testCommentRequest;

    @BeforeEach
    void setUp() {
        // Mock JWT
        mockJwt = Jwt.withTokenValue("mock-token-value")
                .header("alg", "RS256")
                .subject("user-uuid-123")
                .claim("email", "doctor@medilabo.fr")
                .claim("name", "Dr. Jean Dupont")
                .claim("firstName", "Jean")
                .claim("lastName", "Dupont")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // Configurer MockMvc avec un ArgumentResolver personnalisé pour injecter le JWT
        mockMvc = MockMvcBuilders.standaloneSetup(noteAttachmentController)
                .setCustomArgumentResolvers(new JwtArgumentResolver(mockJwt))
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        // Test FileResponse
        testFileResponse = FileResponse.builder()
                .fileUuid("file-uuid-123")
                .name("rapport.pdf")
                .extension("pdf")
                .contentType("application/pdf")
                .size(1024L)
                .formattedSize("1 KB")
                .downloadUrl("/api/notes/note-uuid-456/files/file-uuid-123/download")
                .uploadedByUuid("user-uuid-123")
                .uploadedByName("Dr. Jean Dupont")
                .uploadedByRole("DOCTOR")
                .uploadedAt(LocalDateTime.now())
                .build();

        // Test CommentResponse
        testCommentResponse = CommentResponse.builder()
                .commentUuid("comment-uuid-789")
                .content("Ceci est un commentaire de test")
                .authorUuid("user-uuid-123")
                .authorName("Dr. Jean Dupont")
                .authorRole("DOCTOR")
                .edited(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Test CommentRequest
        testCommentRequest = CommentRequest.builder()
                .content("Ceci est un commentaire de test")
                .build();
    }

    /**ArgumentResolver personnalisé pour injecter le JWT dans les tests*/
    static class JwtArgumentResolver implements HandlerMethodArgumentResolver {

        private final Jwt jwt;

        JwtArgumentResolver(Jwt jwt) {
            this.jwt = jwt;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(Jwt.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return jwt;
        }
    }

    // FICHIERS - UPLOAD
    @Nested
    @DisplayName("POST /api/notes/{noteUuid}/files")
    class UploadFileEndpoint {

        @Test
        @DisplayName("Should upload file successfully")
        void uploadFile_validFile_returns200() throws Exception {
            MockMultipartFile file = new MockMultipartFile("file", "rapport.pdf", "application/pdf", "PDF content".getBytes());

            when(noteFileService.uploadFile(eq("note-uuid-456"), any(), any(Jwt.class)))
                    .thenReturn(Mono.just(testFileResponse));

            MvcResult mvcResult = mockMvc.perform(multipart("/api/notes/note-uuid-456/files")
                            .file(file))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.file.fileUuid", is("file-uuid-123")))
                    .andExpect(jsonPath("$.data.file.name", is("rapport.pdf")))
                    .andExpect(jsonPath("$.message", containsString("uploadé")));

            verify(noteFileService).uploadFile(eq("note-uuid-456"), any(), any(Jwt.class));
        }
    }

    //  FICHIERS - LIST
    @Nested
    @DisplayName("GET /api/notes/{noteUuid}/files")
    class GetFilesEndpoint {

        @Test
        @DisplayName("Should return list of files")
        void getFiles_filesExist_returns200() throws Exception {
            when(noteFileService.getFiles("note-uuid-456"))
                    .thenReturn(Flux.just(testFileResponse));

            MvcResult mvcResult = mockMvc.perform(get("/api/notes/note-uuid-456/files"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.files", hasSize(1)))
                    .andExpect(jsonPath("$.data.count", is(1)))
                    .andExpect(jsonPath("$.data.files[0].fileUuid", is("file-uuid-123")))
                    .andExpect(jsonPath("$.message", containsString("récupérés")));

            verify(noteFileService).getFiles("note-uuid-456");
        }

        @Test
        @DisplayName("Should return empty list when no files")
        void getFiles_noFiles_returns200WithEmptyList() throws Exception {
            when(noteFileService.getFiles("note-uuid-456"))
                    .thenReturn(Flux.empty());

            MvcResult mvcResult = mockMvc.perform(get("/api/notes/note-uuid-456/files"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.files", hasSize(0)))
                    .andExpect(jsonPath("$.data.count", is(0)));

            verify(noteFileService).getFiles("note-uuid-456");
        }
    }

    //  FICHIERS - DOWNLOAD

    @Nested
    @DisplayName("GET /api/notes/{noteUuid}/files/{fileUuid}/download")
    class DownloadFileEndpoint {

        @Test
        @DisplayName("Should download file successfully")
        void downloadFile_fileExists_returns200() throws Exception {
            byte[] fileContent = "PDF content".getBytes();
            Resource resource = new ByteArrayResource(fileContent);

            FileDownload downloadResponse = FileDownload.builder()
                    .resource(resource)
                    .filename("rapport.pdf")
                    .contentType("application/pdf")
                    .build();

            when(noteFileService.downloadFile("note-uuid-456", "file-uuid-123"))
                    .thenReturn(Mono.just(downloadResponse));

            MvcResult mvcResult = mockMvc.perform(get("/api/notes/note-uuid-456/files/file-uuid-123/download"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", containsString("rapport.pdf")))
                    .andExpect(content().contentType("application/pdf"));

            verify(noteFileService).downloadFile("note-uuid-456", "file-uuid-123");
        }
    }

    //  FICHIERS - DELETE
    @Nested
    @DisplayName("DELETE /api/notes/{noteUuid}/files/{fileUuid}")
    class DeleteFileEndpoint {

        @Test
        @DisplayName("Should delete file successfully")
        void deleteFile_fileExists_returns200() throws Exception {
            when(noteFileService.deleteFile(eq("note-uuid-456"), eq("file-uuid-123"), any(Jwt.class)))
                    .thenReturn(Mono.empty());

            MvcResult mvcResult = mockMvc.perform(delete("/api/notes/note-uuid-456/files/file-uuid-123"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("supprimé")));

            verify(noteFileService).deleteFile(eq("note-uuid-456"), eq("file-uuid-123"), any(Jwt.class));
        }
    }

    // COMMENTAIRES - ADD
    @Nested
    @DisplayName("POST /api/notes/{noteUuid}/comments")
    class AddCommentEndpoint {

        @Test
        @DisplayName("Should add comment successfully")
        void addComment_validRequest_returns200() throws Exception {
            when(noteCommentService.addComment(eq("note-uuid-456"), any(CommentRequest.class), any(Jwt.class)))
                    .thenReturn(Mono.just(testCommentResponse));

            MvcResult mvcResult = mockMvc.perform(post("/api/notes/note-uuid-456/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testCommentRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.comment.commentUuid", is("comment-uuid-789")))
                    .andExpect(jsonPath("$.data.comment.content", is("Ceci est un commentaire de test")))
                    .andExpect(jsonPath("$.message", containsString("ajouté")));

            verify(noteCommentService).addComment(eq("note-uuid-456"), any(CommentRequest.class), any(Jwt.class));
        }
    }

    // COMMENTAIRES - LIST

    @Nested
    @DisplayName("GET /api/notes/{noteUuid}/comments")
    class GetCommentsEndpoint {

        @Test
        @DisplayName("Should return list of comments")
        void getComments_commentsExist_returns200() throws Exception {
            when(noteCommentService.getComments("note-uuid-456"))
                    .thenReturn(Flux.just(testCommentResponse));

            MvcResult mvcResult = mockMvc.perform(get("/api/notes/note-uuid-456/comments"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.comments", hasSize(1)))
                    .andExpect(jsonPath("$.data.count", is(1)))
                    .andExpect(jsonPath("$.data.comments[0].commentUuid", is("comment-uuid-789")))
                    .andExpect(jsonPath("$.message", containsString("récupérés")));

            verify(noteCommentService).getComments("note-uuid-456");
        }

        @Test
        @DisplayName("Should return empty list when no comments")
        void getComments_noComments_returns200WithEmptyList() throws Exception {
            when(noteCommentService.getComments("note-uuid-456"))
                    .thenReturn(Flux.empty());

            MvcResult mvcResult = mockMvc.perform(get("/api/notes/note-uuid-456/comments"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.comments", hasSize(0)))
                    .andExpect(jsonPath("$.data.count", is(0)));

            verify(noteCommentService).getComments("note-uuid-456");
        }
    }

    // COMMENTAIRES - UPDATE

    @Nested
    @DisplayName("PUT /api/notes/{noteUuid}/comments/{commentUuid}")
    class UpdateCommentEndpoint {

        @Test
        @DisplayName("Should update comment successfully")
        void updateComment_validRequest_returns200() throws Exception {
            CommentResponse updatedComment = testCommentResponse.toBuilder()
                    .content("Commentaire modifié")
                    .edited(true)
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(noteCommentService.updateComment(eq("note-uuid-456"), eq("comment-uuid-789"),
                    any(CommentRequest.class), any(Jwt.class)))
                    .thenReturn(Mono.just(updatedComment));

            CommentRequest updateRequest = CommentRequest.builder()
                    .content("Commentaire modifié")
                    .build();

            MvcResult mvcResult = mockMvc.perform(put("/api/notes/note-uuid-456/comments/comment-uuid-789")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.comment.content", is("Commentaire modifié")))
                    .andExpect(jsonPath("$.message", containsString("modifié")));

            verify(noteCommentService).updateComment(eq("note-uuid-456"), eq("comment-uuid-789"),
                    any(CommentRequest.class), any(Jwt.class));
        }
    }

    // COMMENTAIRES - DELETE

    @Nested
    @DisplayName("DELETE /api/notes/{noteUuid}/comments/{commentUuid}")
    class DeleteCommentEndpoint {

        @Test
        @DisplayName("Should delete comment successfully")
        void deleteComment_commentExists_returns200() throws Exception {
            when(noteCommentService.deleteComment(eq("note-uuid-456"), eq("comment-uuid-789"), any(Jwt.class)))
                    .thenReturn(Mono.empty());

            MvcResult mvcResult = mockMvc.perform(delete("/api/notes/note-uuid-456/comments/comment-uuid-789"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("supprimé")));

            verify(noteCommentService).deleteComment(eq("note-uuid-456"), eq("comment-uuid-789"), any(Jwt.class));
        }
    }
}