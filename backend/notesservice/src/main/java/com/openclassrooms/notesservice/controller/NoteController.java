package com.openclassrooms.notesservice.controller;

import com.openclassrooms.notesservice.domain.Response;
import com.openclassrooms.notesservice.dto.NoteRequest;
import com.openclassrooms.notesservice.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.openclassrooms.notesservice.constant.Role.*;
import static com.openclassrooms.notesservice.util.RequestUtils.getResponse;
import static org.springframework.http.HttpStatus.*;

/**
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
@Tag(name = "Notes", description = "API de gestion des notes médicales")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping("/page")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<Response>> getAllNotesPageable(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest request) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return noteService.getAllActiveNotesPageable(pageable)
                .map(notePage -> ResponseEntity.ok(getResponse(request, Map.of(
                        "notes", notePage.getContent(),
                        "currentPage", notePage.getNumber(),
                        "totalPages", notePage.getTotalPages(),
                        "totalElements", notePage.getTotalElements(),
                        "size", notePage.getSize()
                ), "Notes récupérées avec succès", OK)));
    }

    @Operation(summary = "Récupérer toutes les notes actives")
    @GetMapping
    @PreAuthorize(ALL_STAFF)
    public Mono<ResponseEntity<Response>> getAllNotes(HttpServletRequest request) {
        log.debug("Fetching all notes");
        return noteService.getAllActiveNotes()
                .collectList()
                .map(notes -> ResponseEntity.ok(
                        getResponse(request, Map.of("notes", notes, "count", notes.size()),
                                "Notes récupérées avec succès", OK)));
    }

    @Operation(summary = "Créer une nouvelle note",
            description = "Ajoute une note d'observation à l'historique du patient")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Note créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Accès refusé - Réservé aux praticiens")
    })
    @PostMapping
    @PreAuthorize(PRACTITIONER_ONLY)
    public Mono<ResponseEntity<Response>> createNote(@Valid @RequestBody NoteRequest request, @Parameter(hidden = true) Authentication authentication, HttpServletRequest httpRequest) {

        String practitionerUuid = authentication.getName();
        String practitionerName = extractPractitionerName(authentication);

        log.info("Practitioner {} creating note for patient {}", practitionerUuid, request.getPatientUuid());

        return noteService.createNote(request, practitionerUuid, practitionerName)
                .map(note -> {
                    URI location = URI.create("/api/notes/" + note.getNoteUuid());
                    return ResponseEntity.created(location)
                            .body(getResponse(httpRequest, Map.of("note", note), "Note créée avec succès", CREATED));
                });
    }

    @Operation(summary = "Récupérer mes notes médicales (vue patient)")
    @GetMapping("/my-records")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<Response>> getMyMedicalNotes(
            @Parameter(hidden = true) Authentication authentication,
            HttpServletRequest request) {

        String userUuid = authentication.getName();
        log.debug("Patient fetching own medical notes, userUuid: {}", userUuid);

        return noteService.getNotesByUserUuid(userUuid)
                .collectList()
                .map(notes -> ResponseEntity.ok(
                        getResponse(request, Map.of("notes", notes, "count", notes.size()),
                                "Notes récupérées avec succès", OK)));
    }

    @Operation(summary = "Récupérer une note par UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Note trouvée"),
            @ApiResponse(responseCode = "404", description = "Note introuvable")
    })
    @GetMapping("/{noteUuid}")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<Response>> getNoteByUuid(@Parameter(description = "UUID de la note") @PathVariable String noteUuid, HttpServletRequest request) {

        log.debug("Fetching note: {}", noteUuid);
        return noteService.getNoteByUuid(noteUuid).map(note -> ResponseEntity.ok(getResponse(request, Map.of("note", note), "Note récupérée avec succès", OK)));
    }

    @Operation(summary = "Récupérer l'historique des notes d'un patient",
            description = "Retourne toutes les notes d'observation du patient, triées par date décroissante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique récupéré")
    })
    @GetMapping("/patient/{patientUuid}")
    @PreAuthorize(ALL_STAFF)
    public Mono<ResponseEntity<Response>> getNotesByPatientUuid(@Parameter(description = "UUID du patient") @PathVariable String patientUuid, HttpServletRequest request) {

        log.debug("Fetching notes for patient: {}", patientUuid);
        return noteService.getNotesByPatientUuid(patientUuid).collectList().map(notes -> ResponseEntity.ok(getResponse(request, Map.of("notes", notes, "count", notes.size()), "Historique récupéré avec succès", OK)));
    }

    @Operation(summary = "Récupérer les notes créées par un praticien")
    @GetMapping("/practitioner/{practitionerUuid}")
    @PreAuthorize(ADMIN_ONLY)
    public Mono<ResponseEntity<Response>> getNotesByPractitionerUuid(@Parameter(description = "UUID du praticien") @PathVariable String practitionerUuid, HttpServletRequest request) {

        log.debug("Fetching notes by practitioner: {}", practitionerUuid);
        return noteService.getNotesByPractitionerUuid(practitionerUuid).collectList().map(notes -> ResponseEntity.ok(getResponse(request, Map.of("notes", notes, "count", notes.size()), "Notes récupérées avec succès", OK)));
    }

    @Operation(summary = "Récupérer mes propres notes",
            description = "Retourne les notes créées par le praticien connecté")
    @GetMapping("/my-notes")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public Mono<ResponseEntity<Response>> getMyNotes(@Parameter(hidden = true) Authentication authentication, HttpServletRequest request) {

        String practitionerUuid = authentication.getName();
        log.debug("Fetching notes for current practitioner: {}", practitionerUuid);
        return noteService.getNotesByPractitionerUuid(practitionerUuid).collectList().map(notes -> ResponseEntity.ok(getResponse(request, Map.of("notes", notes, "count", notes.size()), "Mes notes récupérées avec succès", OK)));
    }

    @Operation(summary = "Mettre à jour une note",
            description = "Seul l'auteur de la note peut la modifier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Note mise à jour"),
            @ApiResponse(responseCode = "403", description = "Non autorisé à modifier cette note"),
            @ApiResponse(responseCode = "404", description = "Note introuvable")
    })
    @PutMapping("/{noteUuid}")
    @PreAuthorize(ALL_STAFF)
    public Mono<ResponseEntity<Response>> updateNote(@PathVariable String noteUuid, @Valid @RequestBody NoteRequest request, Authentication authentication, HttpServletRequest httpRequest) {

        String practitionerUuid = authentication.getName();
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SUPER_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));

        log.info("Practitioner {} updating note {}", practitionerUuid, noteUuid);
        return noteService.updateNote(noteUuid, request, practitionerUuid, isSuperAdmin)
                .map(note -> ResponseEntity.ok(getResponse(httpRequest, Map.of("note", note), "Note mise à jour avec succès", OK)));
    }

    @Operation(summary = "Supprimer une note (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Note supprimée"),
            @ApiResponse(responseCode = "404", description = "Note introuvable")
    })
    @DeleteMapping("/{noteUuid}")
    @PreAuthorize(ADMIN_OR_PRACTITIONER)
    public Mono<ResponseEntity<Response>> deleteNote(@Parameter(description = "UUID de la note") @PathVariable String noteUuid, HttpServletRequest request) {

        log.info("Deleting note: {}", noteUuid);
        return noteService.deleteNote(noteUuid).then(Mono.just(ResponseEntity.ok(getResponse(request, Map.of(), "Note supprimée avec succès", OK))));
    }

    @Operation(summary = "Compter les notes d'un patient")
    @GetMapping("/count/patient/{patientUuid}")
    @PreAuthorize(ALL_STAFF)
    public Mono<ResponseEntity<Response>> countNotesByPatientUuid(@Parameter(description = "UUID du patient") @PathVariable String patientUuid, HttpServletRequest request) {

        log.debug("Counting notes for patient: {}", patientUuid);
        return noteService.countNotesByPatientUuid(patientUuid).map(count -> ResponseEntity.ok(getResponse(request, Map.of("patientUuid", patientUuid, "noteCount", count), "Comptage effectué", OK)));
    }

    /**
     * Extrait le nom du praticien depuis le JWT.
     */
    private String extractPractitionerName(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return "Unknown";
        }

        String fullName = String.format("%s %s",
                jwt.getClaimAsString("firstName"),
                jwt.getClaimAsString("lastName")).trim();

        if (!fullName.isEmpty() && !fullName.equals("null null")) {
            return fullName;
        }

        return Stream.of("name", "preferred_username", "sub")
                .map(jwt::getClaimAsString)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Unknown");
    }
}