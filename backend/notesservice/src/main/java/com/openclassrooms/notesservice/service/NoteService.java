package com.openclassrooms.notesservice.service;

import com.openclassrooms.notesservice.dto.NoteRequest;
import com.openclassrooms.notesservice.dto.NoteResponse;

import java.util.List;

/**
 * Service de gestion des notes médicales.
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-02
 */
public interface NoteService {

    /**
     * Crée une nouvelle note pour un patient.
     *
     * @param request          Contenu de la note
     * @param practitionerUuid UUID du praticien
     * @param practitionerName Nom du praticien
     * @return NoteResponse avec les informations de la note créée
     */
    NoteResponse createNote(NoteRequest request, String practitionerUuid, String practitionerName);

    /**
     * Récupère une note par son UUID.
     *
     * @param noteUuid UUID de la note
     * @return NoteResponse
     */
    NoteResponse getNoteByUuid(String noteUuid);

    /**
     * Récupère toutes les notes d'un patient (historique).
     *
     * @param patientUuid UUID du patient
     * @return Liste des notes
     */
    List<NoteResponse> getNotesByPatientUuid(String patientUuid);

    /**
     * Récupère toutes les notes créées par un praticien.
     *
     * @param practitionerUuid UUID du praticien
     * @return Liste des notes
     */
    List<NoteResponse> getNotesByPractitionerUuid(String practitionerUuid);

    /**
     * Met à jour une note existante.
     *
     * @param noteUuid         UUID de la note
     * @param request          Nouveau contenu
     * @param practitionerUuid UUID du praticien (pour vérification)
     * @return NoteResponse mis à jour
     */
    NoteResponse updateNote(String noteUuid, NoteRequest request, String practitionerUuid);

    /**
     * Supprime une note (soft delete).
     *
     * @param noteUuid UUID de la note
     */
    void deleteNote(String noteUuid);

    /**
     * Compte le nombre de notes pour un patient.
     *
     * @param patientUuid UUID du patient
     * @return Nombre de notes
     */
    long countNotesByPatientUuid(String patientUuid);
}