package com.openclassrooms.notesservice.mapper;

import com.openclassrooms.notesservice.dto.*;
import com.openclassrooms.notesservice.model.Comment;
import com.openclassrooms.notesservice.model.FileAttachment;
import com.openclassrooms.notesservice.model.Note;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, imports = UUID.class)
public interface NoteMapper {

    //  Note
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "noteUuid", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "patientUuid", source = "request.patientUuid")
    @Mapping(target = "practitionerUuid", source = "practitionerUuid")
    @Mapping(target = "practitionerName", source = "practitionerName")
    @Mapping(target = "content", source = "request.content")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "files", ignore = true)
    @Mapping(target = "comments", ignore = true)
    Note toEntity(NoteRequest request, String practitionerUuid, String practitionerName);

    @Mapping(target = "fileCount", expression = "java(note.getFileCount())")
    @Mapping(target = "commentCount", expression = "java(note.getCommentCount())")
    @Mapping(target = "files", source = "files")
    @Mapping(target = "comments", source = "comments")
    NoteResponse toResponse(Note note);

    List<NoteResponse> toResponseList(List<Note> notes);

    // Comment
    @Mapping(target = "commentUuid", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "content", source = "request.content")
    @Mapping(target = "authorUuid", source = "authorUuid")
    @Mapping(target = "authorName", source = "authorName")
    @Mapping(target = "authorRole", source = "authorRole")
    @Mapping(target = "authorImageUrl", source = "authorImageUrl")
    @Mapping(target = "edited", constant = "false")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    Comment toComment(CommentRequest request, String authorUuid, String authorName, String authorRole, String authorImageUrl);

    CommentResponse toCommentResponse(Comment comment);

    List<CommentResponse> toCommentResponseList(List<Comment> comments);

    //  File
    @Mapping(target = "name", source = "originalName")
    @Mapping(target = "downloadUrl", ignore = true) // set dans le service
    FileResponse toFileResponse(FileAttachment file);

    List<FileResponse> toFileResponseList(List<FileAttachment> files);
}