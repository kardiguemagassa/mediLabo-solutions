package com.openclassrooms.assessmentservice.service;

import com.openclassrooms.assessmentservice.model.Gender;
import com.openclassrooms.assessmentservice.model.RiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskLevelCalculator - Calcul du niveau de risque de diabète")
class RiskLevelCalculatorTest {

    private final RiskLevelCalculator calculator = new RiskLevelCalculator();

    // AUCUN RISQUE (NONE)

    @Nested
    @DisplayName("Aucun risque (NONE)")
    class NoneRiskTests {

        @Test
        @DisplayName("0 déclencheur → NONE quel que soit l'âge ou le genre")
        void shouldReturnNone_whenZeroTriggers() {
            assertThat(calculator.calculate(25, Gender.MALE, 0)).isEqualTo(RiskLevel.NONE);
            assertThat(calculator.calculate(25, Gender.FEMALE, 0)).isEqualTo(RiskLevel.NONE);
            assertThat(calculator.calculate(45, Gender.MALE, 0)).isEqualTo(RiskLevel.NONE);
            assertThat(calculator.calculate(45, Gender.FEMALE, 0)).isEqualTo(RiskLevel.NONE);
        }

        @Test
        @DisplayName("1 déclencheur → NONE quel que soit l'âge ou le genre")
        void shouldReturnNone_whenOneTrigger() {
            assertThat(calculator.calculate(25, Gender.MALE, 1)).isEqualTo(RiskLevel.NONE);
            assertThat(calculator.calculate(35, Gender.FEMALE, 1)).isEqualTo(RiskLevel.NONE);
        }
    }

    // RISQUE LIMITÉ (BORDERLINE)

    @Nested
    @DisplayName("Risque limité (BORDERLINE)")
    class BorderlineRiskTests {

        @ParameterizedTest
        @CsvSource({"31, MALE, 2", "31, FEMALE, 2", "45, MALE, 3", "50, FEMALE, 4", "60, MALE, 5"})
        @DisplayName("Patient > 30 ans avec 2-5 déclencheurs → BORDERLINE")
        void shouldReturnBorderline_whenOver30With2To5Triggers(int age, Gender gender, int triggers) {
            RiskLevel result = calculator.calculate(age, gender, triggers);

            assertThat(result).isEqualTo(RiskLevel.BORDERLINE);
        }

        @Test
        @DisplayName("Patient ≤ 30 ans avec 2 déclencheurs → NONE (pas BORDERLINE)")
        void shouldNotReturnBorderline_whenUnder30With2Triggers() {
            assertThat(calculator.calculate(30, Gender.MALE, 2)).isEqualTo(RiskLevel.NONE);
            assertThat(calculator.calculate(25, Gender.FEMALE, 2)).isEqualTo(RiskLevel.NONE);
        }
    }

    // DANGER (IN_DANGER)

    @Nested
    @DisplayName("Danger (IN_DANGER)")
    class InDangerRiskTests {

        @Test
        @DisplayName("Homme < 30 ans avec 3-4 déclencheurs → IN_DANGER")
        void shouldReturnInDanger_whenMaleUnder30With3To4Triggers() {
            assertThat(calculator.calculate(25, Gender.MALE, 3)).isEqualTo(RiskLevel.IN_DANGER);
            assertThat(calculator.calculate(29, Gender.MALE, 4)).isEqualTo(RiskLevel.IN_DANGER);
        }

        @Test
        @DisplayName("Femme < 30 ans avec 4-6 déclencheurs → IN_DANGER")
        void shouldReturnInDanger_whenFemaleUnder30With4To6Triggers() {
            assertThat(calculator.calculate(25, Gender.FEMALE, 4)).isEqualTo(RiskLevel.IN_DANGER);
            assertThat(calculator.calculate(29, Gender.FEMALE, 5)).isEqualTo(RiskLevel.IN_DANGER);
            assertThat(calculator.calculate(28, Gender.FEMALE, 6)).isEqualTo(RiskLevel.IN_DANGER);
        }

        @Test
        @DisplayName("Patient > 30 ans avec 6-7 déclencheurs → IN_DANGER")
        void shouldReturnInDanger_whenOver30With6To7Triggers() {
            assertThat(calculator.calculate(35, Gender.MALE, 6)).isEqualTo(RiskLevel.IN_DANGER);
            assertThat(calculator.calculate(45, Gender.FEMALE, 7)).isEqualTo(RiskLevel.IN_DANGER);
        }
    }

    // APPARITION PRÉCOCE (EARLY_ONSET)

    @Nested
    @DisplayName("Apparition précoce (EARLY_ONSET)")
    class EarlyOnsetRiskTests {

        @Test
        @DisplayName("Homme < 30 ans avec ≥5 déclencheurs → EARLY_ONSET")
        void shouldReturnEarlyOnset_whenMaleUnder30With5OrMoreTriggers() {
            assertThat(calculator.calculate(25, Gender.MALE, 5)).isEqualTo(RiskLevel.EARLY_ONSET);
            assertThat(calculator.calculate(29, Gender.MALE, 6)).isEqualTo(RiskLevel.EARLY_ONSET);
            assertThat(calculator.calculate(20, Gender.MALE, 10)).isEqualTo(RiskLevel.EARLY_ONSET);
        }

        @Test
        @DisplayName("Femme < 30 ans avec ≥7 déclencheurs → EARLY_ONSET")
        void shouldReturnEarlyOnset_whenFemaleUnder30With7OrMoreTriggers() {
            assertThat(calculator.calculate(25, Gender.FEMALE, 7)).isEqualTo(RiskLevel.EARLY_ONSET);
            assertThat(calculator.calculate(29, Gender.FEMALE, 8)).isEqualTo(RiskLevel.EARLY_ONSET);
            assertThat(calculator.calculate(20, Gender.FEMALE, 12)).isEqualTo(RiskLevel.EARLY_ONSET);
        }

        @Test
        @DisplayName("Patient > 30 ans avec ≥8 déclencheurs → EARLY_ONSET")
        void shouldReturnEarlyOnset_whenOver30With8OrMoreTriggers() {
            assertThat(calculator.calculate(35, Gender.MALE, 8)).isEqualTo(RiskLevel.EARLY_ONSET);
            assertThat(calculator.calculate(50, Gender.FEMALE, 9)).isEqualTo(RiskLevel.EARLY_ONSET);
            assertThat(calculator.calculate(65, Gender.MALE, 12)).isEqualTo(RiskLevel.EARLY_ONSET);
        }
    }

    //  CAS LIMITES

    @Nested
    @DisplayName("Cas limites")
    class EdgeCasesTests {

        @Test
        @DisplayName("Âge exactement 30 ans = considéré comme ≤ 30")
        void shouldTreatAge30AsUnder30() {
            // Homme 30 ans, 3 déclencheurs → IN_DANGER (règle < 30 ans)
            assertThat(calculator.calculate(30, Gender.MALE, 3)).isEqualTo(RiskLevel.IN_DANGER);
        }

        @Test
        @DisplayName("Âge exactement 31 ans = considéré comme > 30")
        void shouldTreatAge31AsOver30() {
            // Homme 31 ans, 3 déclencheurs → BORDERLINE (règle > 30 ans)
            assertThat(calculator.calculate(31, Gender.MALE, 3)).isEqualTo(RiskLevel.BORDERLINE);
        }
    }
}