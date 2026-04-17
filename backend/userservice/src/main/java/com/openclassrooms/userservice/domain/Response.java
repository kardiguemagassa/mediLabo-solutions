package com.openclassrooms.userservice.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.openclassrooms.userservice.exception.HandleException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Représentation standardisée des réponses REST de l’API
 * Cette classe {@code Response} sert à envelopper toutes les réponses envoyées par l’API,
 * qu’il s’agisse de succès ou d’erreurs. Elle permet de garantir une structure uniforme
 * pour le frontend et les consommateurs de l’API (React, Angular, mobile, etc.).
 * Les champs principaux :
 * {@code time} : date et heure de la réponse, utile pour le debug et le logging côté client
 * {@code code} : code HTTP associé à la réponse (ex : 200, 400, 500)
 * {@code path} : URI de la requête ayant généré la réponse
 * {@code status} : {@link HttpStatus} détaillant le statut HTTP
 * {@code message} : message compréhensible par l’utilisateur ou le client
 * {@code exception} : nom ou message de l’exception levée, utile pour le debug
 * {@code data} : données supplémentaires retournées par l’API, sous forme de Map
 * Notes :
 * L’annotation {@link com.fasterxml.jackson.annotation.JsonInclude} assure que les champs
 *         ayant leur valeur par défaut ne sont pas sérialisés dans le JSON
 *  Cette structure permet au frontend de traiter uniformément les réponses, qu’il s’agisse
 *         de données métier, d’erreurs ou de messages informatifs
 * Elle est utilisée par le {@link HandleException}
 *         pour renvoyer les erreurs sous une forme standardisée
 *
 * Exemple de réponse JSON
 * {@code
 * {
 *   "time": "2026-01-12T12:34:56Z",
 *   "code": 400,
 *   "path": "/api/login",
 *   "status": "BAD_REQUEST",
 *   "message": "Adresse e-mail ou mot de passe incorrect",
 *   "exception": "BadCredentialsException",
 *   "data": null
 * }
 * }
 *
 * Cette classe rend l’API plus robuste, plus lisible et plus facile à maintenir
 * pour les développeurs backend et frontend.
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record Response(String time, int code, String path, HttpStatus status, String message, String exception, Map<?, ?> data) {}
