package com.openclassrooms.notesservice.service;

import com.openclassrooms.notesservice.dto.NoteRequest;
import com.openclassrooms.notesservice.dto.NoteResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service de gestion des notes médicales (Full Réactif).
 *
 * @author Kardigué MAGASSA
 * @version 2.0
 * @since 2026-02-25
 */
public interface NoteService {

    /**
     * Crée une nouvelle note pour un patient.
     */
    Mono<NoteResponse> createNote(NoteRequest request, String practitionerUuid, String practitionerName);

    /**
     * Récupère une note par son UUID.
     */
    Mono<NoteResponse> getNoteByUuid(String noteUuid);

    /**
     * Récupère toutes les notes d'un patient (historique).
     */
    Flux<NoteResponse> getNotesByPatientUuid(String patientUuid);

    /**
     * Récupère toutes les notes créées par un praticien.
     */
    Flux<NoteResponse> getNotesByPractitionerUuid(String practitionerUuid);

    /**
     * Met à jour une note existante.
     */
    Mono<NoteResponse> updateNote(String noteUuid, NoteRequest request, String practitionerUuid);

    /**
     * Supprime une note (soft delete).
     */
    Mono<Void> deleteNote(String noteUuid);

    /**
     * Compte le nombre de notes pour un patient.
     */
    Mono<Long> countNotesByPatientUuid(String patientUuid);
}