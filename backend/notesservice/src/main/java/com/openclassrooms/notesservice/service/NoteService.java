package com.openclassrooms.notesservice.service;

import com.openclassrooms.notesservice.dto.NoteRequest;
import com.openclassrooms.notesservice.dto.NoteResponse;

import java.util.List;

/**
 * Service de gestion des notes médicales
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-02
 */

public interface NoteService {

    /**
     * Crée une nouvelle note pour un patient
     */
    NoteResponse createNote(NoteRequest request, String practitionerUuid, String practitionerName);
    /**
     * Récupère une note par son UUID
     */
    NoteResponse getNoteByUuid(String noteUuid);
    /**
     * Récupère toutes les notes d'un patient (historique)
     */
    List<NoteResponse> getNotesByPatientUuid(String patientUuid);
    /**
     * Récupère toutes les notes créées par un praticien
     */
    List<NoteResponse> getNotesByPractitionerUuid(String practitionerUuid);
    /**
     * Met à jour une note existante
     */
    NoteResponse updateNote(String noteUuid, NoteRequest request, String practitionerUuid);
    /**
     * Supprime une note (soft delete)
     */
    void deleteNote(String noteUuid);
    /**
     * Compte le nombre de notes pour un patient
     */
    long countNotesByPatientUuid(String patientUuid);
}