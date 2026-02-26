package com.openclassrooms.assessmentservice.service;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Termes déclencheurs pour l'évaluation du risque de diabète.
 * Ces termes sont recherchés dans les notes médicales
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
public final class TriggerTerms {

    private TriggerTerms() {}

    /**
     * Liste des termes déclencheurs selon les spécifications métier.
     */
    public static final List<String> TERMS = List.of("Hémoglobine A1C", "Microalbumine", "Taille", "Poids", "Fumeur", "Fumeuse", "Anormal", "Cholestérol", "Vertiges", "Rechute", "Réaction", "Anticorps");

    /**
     * Patterns compilés pour recherche efficace (insensible à la casse).
     */
    public static final List<Pattern> PATTERNS = TERMS.stream().map(term -> Pattern.compile(Pattern.quote(term), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)).toList();

    /**
     * Compte le nombre de termes déclencheurs uniques dans un texte.
     *
     * @param text le texte à analyser
     * @return le nombre de termes trouvés
     */
    public static int countTriggers(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) PATTERNS.stream().filter(pattern -> pattern.matcher(text).find()).count();
    }

    /**
     * Trouve tous les termes déclencheurs présents dans un texte.
     *
     * @param text le texte à analyser
     * @return ensemble des termes trouvés
     */
    public static Set<String> findTriggers(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return TERMS.stream().filter(term -> Pattern.compile(Pattern.quote(term), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text).find()).collect(Collectors.toSet());
    }

    /**
     * Compte les termes déclencheurs uniques dans plusieurs textes.
     *
     * @param texts liste de textes à analyser
     * @return le nombre total de termes uniques trouvés
     */
    public static int countTriggersInMultipleTexts(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return 0;
        }
        String combinedText = String.join(" ", texts);
        return countTriggers(combinedText);
    }

    /**
     * Trouve tous les termes déclencheurs dans plusieurs textes.
     *
     * @param texts liste de textes à analyser
     * @return ensemble des termes uniques trouvés
     */
    public static Set<String> findTriggersInMultipleTexts(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Set.of();
        }
        String combinedText = String.join(" ", texts);
        return findTriggers(combinedText);
    }
}
