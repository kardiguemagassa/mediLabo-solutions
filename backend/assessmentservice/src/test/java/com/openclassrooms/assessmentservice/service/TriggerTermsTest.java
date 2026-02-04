package com.openclassrooms.assessmentservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TriggerTerms - Détection des termes déclencheurs")
class TriggerTermsTest {

    @Nested
    @DisplayName("countTriggers()")
    class CountTriggersTests {

        @Test
        @DisplayName("Devrait retourner 0 pour un texte sans déclencheurs")
        void shouldReturnZeroWhenNoTriggers() {
            String text = "Le patient va bien, aucun problème détecté.";

            int count = TriggerTerms.countTriggers(text);

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Devrait compter un seul déclencheur")
        void shouldCountSingleTrigger() {
            String text = "Le patient est fumeur depuis 10 ans.";

            int count = TriggerTerms.countTriggers(text);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Devrait compter plusieurs déclencheurs")
        void shouldCountMultipleTriggers() {
            String text = "Patient fumeur, taille 180cm, poids 85kg. Cholestérol élevé.";

            int count = TriggerTerms.countTriggers(text);

            assertThat(count).isEqualTo(4); // Fumeur, Taille, Poids, Cholestérol
        }

        @Test
        @DisplayName("Devrait être insensible à la casse")
        void shouldBeCaseInsensitive() {
            String text = "FUMEUR, Cholestérol, hémoglobine a1c";

            int count = TriggerTerms.countTriggers(text);

            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Devrait compter une seule fois un terme répété")
        void shouldCountRepeatedTermOnce() {
            String text = "Fumeur. Le patient est fumeur. Encore fumeur.";

            int count = TriggerTerms.countTriggers(text);

            assertThat(count).isEqualTo(1);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Devrait retourner 0 pour texte null ou vide")
        void shouldReturnZeroForNullOrEmpty(String text) {
            int count = TriggerTerms.countTriggers(text);

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Devrait détecter Fumeur et Fumeuse séparément")
        void shouldDetectFumeurAndFumeuse() {
            String text = "Le patient est fumeur et sa femme est fumeuse.";

            int count = TriggerTerms.countTriggers(text);

            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("findTriggers()")
    class FindTriggersTests {

        @Test
        @DisplayName("Devrait retourner les termes trouvés")
        void shouldReturnFoundTerms() {
            String text = "Patient fumeur avec cholestérol anormal.";

            Set<String> triggers = TriggerTerms.findTriggers(text);

            assertThat(triggers).containsExactlyInAnyOrder("Fumeur", "Cholestérol", "Anormal");
        }

        @Test
        @DisplayName("Devrait retourner ensemble vide si aucun déclencheur")
        void shouldReturnEmptySetWhenNoTriggers() {
            String text = "Le patient est en bonne santé.";

            Set<String> triggers = TriggerTerms.findTriggers(text);

            assertThat(triggers).isEmpty();
        }
    }

    @Nested
    @DisplayName("countTriggersInMultipleTexts()")
    class CountTriggersInMultipleTextsTests {

        @Test
        @DisplayName("Devrait compter les déclencheurs uniques dans plusieurs notes")
        void shouldCountUniqueTriggersAcrossNotes() {
            List<String> notes = List.of(
                    "Patient fumeur.",
                    "Cholestérol élevé.",
                    "Fumeur confirmé, poids stable."  // Fumeur déjà compté
            );

            int count = TriggerTerms.countTriggersInMultipleTexts(notes);

            assertThat(count).isEqualTo(3); // Fumeur, Cholestérol, Poids
        }

        @Test
        @DisplayName("Devrait retourner 0 pour liste vide")
        void shouldReturnZeroForEmptyList() {
            int count = TriggerTerms.countTriggersInMultipleTexts(List.of());

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Devrait retourner 0 pour liste null")
        void shouldReturnZeroForNullList() {
            int count = TriggerTerms.countTriggersInMultipleTexts(null);

            assertThat(count).isZero();
        }
    }
}
