package com.openclassrooms.assessmentservice.service;

import com.openclassrooms.assessmentservice.model.Gender;
import com.openclassrooms.assessmentservice.model.RiskLevel;
import org.springframework.stereotype.Component;

/**
 * Calcule le niveau de risque de diabète selon les règles métier.
 *
 * Règles :
 * - NONE : 0-1 déclencheur
 * - BORDERLINE : 2-5 déclencheurs ET âge > 30 ans
 * - IN_DANGER : Dépend de l'âge et du genre
 *   - Homme < 30 ans : 3-4 déclencheurs
 *   - Femme < 30 ans : 4-6 déclencheurs
 *   - > 30 ans : 6-7 déclencheurs
 * - EARLY_ONSET : Dépend de l'âge et du genre
 *   - Homme < 30 ans : ≥5 déclencheurs
 *   - Femme < 30 ans : ≥7 déclencheurs
 *   - > 30 ans : ≥8 déclencheurs
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-02-09
 */
@Component
public class RiskLevelCalculator {

    private static final int AGE_THRESHOLD = 30;

    /**
     * Calcule le niveau de risque de diabète.
     *
     * @param age            âge du patient en années
     * @param gender         genre du patient
     * @param triggerCount   nombre de termes déclencheurs trouvés
     * @return le niveau de risque calculé
     */
    public RiskLevel calculate(int age, Gender gender, int triggerCount) {
        // Aucun risque si 0-1 déclencheur
        if (triggerCount <= 1) {
            return RiskLevel.NONE;
        }

        boolean isYoung = age <= AGE_THRESHOLD;
        boolean isMale = gender == Gender.MALE;

        if (isYoung) {
            return calculateForYoungPatient(isMale, triggerCount);
        } else {
            return calculateForOlderPatient(triggerCount);
        }
    }

    /**
     * Calcul pour patient ≤ 30 ans.
     */
    private RiskLevel calculateForYoungPatient(boolean isMale, int triggerCount) {
        if (isMale) {
            // Homme < 30 ans
            if (triggerCount >= 5) {
                return RiskLevel.EARLY_ONSET;
            } else if (triggerCount >= 3) {
                return RiskLevel.IN_DANGER;
            }
        } else {
            // Femme < 30 ans
            if (triggerCount >= 7) {
                return RiskLevel.EARLY_ONSET;
            } else if (triggerCount >= 4) {
                return RiskLevel.IN_DANGER;
            }
        }

        // 2-3 déclencheurs pour homme jeune ou 2-3 pour femme jeune → NONE
        return RiskLevel.NONE;
    }

    /**
     * Calcul pour patient > 30 ans.
     */
    private RiskLevel calculateForOlderPatient(int triggerCount) {
        if (triggerCount >= 8) {
            return RiskLevel.EARLY_ONSET;
        } else if (triggerCount >= 6) {
            return RiskLevel.IN_DANGER;
        } else if (triggerCount >= 2) {
            return RiskLevel.BORDERLINE;
        }

        return RiskLevel.NONE;
    }
}