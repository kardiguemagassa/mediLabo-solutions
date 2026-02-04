package com.openclassrooms.assessmentservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Genre du patient pour le calcul du risque.
 * Supporte "M/MALE" et "F/FEMALE".
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Getter
@RequiredArgsConstructor
public enum Gender {
    MALE("M"),
    FEMALE("F");

    private final String code;

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static Gender fromString(String value) {
        if (value == null || value.isBlank()) return null;

        return switch (value.toUpperCase()) {
            case "M", "MALE" -> MALE;
            case "F", "FEMALE" -> FEMALE;
            default -> throw new IllegalArgumentException("Unknown gender: " + value);
        };
    }
}