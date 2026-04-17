package com.openclassrooms.assessmentservice.service;

import com.openclassrooms.assessmentservice.model.Gender;
import com.openclassrooms.assessmentservice.model.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Calcule le niveau de risque de diabète via un moteur de règles déclaratives.
 * Les règles sont évaluées par ordre de priorité (EARLY_ONSET, IN_DANGER, BORDERLINE, NONE).
 * La première règle qui match détermine le résultat.
 */
@Component
public class RiskLevelCalculator {

    private static final int AGE_THRESHOLD = 30;

    /** Règle = condition + résultat */
    private record RiskRule(Predicate<RiskContext> condition, RiskLevel level) {}

    /** Contexte d'évaluation */
    private record RiskContext(int age, boolean isMale, int triggerCount) {
        boolean isYoung() { return age <= AGE_THRESHOLD; }
    }

    /**
     * Règles métier ordonnées par sévérité décroissante.
     * Première règle qui match → résultat retourné.
     */
    private static final List<RiskRule> RULES = List.of(
            /** EARLY_ONSET — Homme jeune ≥5 déclencheurs */
            new RiskRule(ctx -> ctx.isYoung() && ctx.isMale() && ctx.triggerCount() >= 5, RiskLevel.EARLY_ONSET),
            /** EARLY_ONSET — Femme jeune ≥7 déclencheurs */
            new RiskRule(ctx -> ctx.isYoung() && !ctx.isMale() && ctx.triggerCount() >= 7, RiskLevel.EARLY_ONSET),
            /** EARLY_ONSET — Patient >30 ans ≥8 déclencheurs */
            new RiskRule(ctx -> !ctx.isYoung() && ctx.triggerCount() >= 8, RiskLevel.EARLY_ONSET),

            /**IN_DANGER — Homme jeune 3-4 déclencheurs */
            new RiskRule(ctx -> ctx.isYoung() && ctx.isMale() && ctx.triggerCount() >= 3, RiskLevel.IN_DANGER),
            /** IN_DANGER — Femme jeune 4-6 déclencheurs*/
            new RiskRule(ctx -> ctx.isYoung() && !ctx.isMale() && ctx.triggerCount() >= 4, RiskLevel.IN_DANGER),
            /** IN_DANGER — Patient >30 ans 6-7 déclencheurs */
            new RiskRule(ctx -> !ctx.isYoung() && ctx.triggerCount() >= 6, RiskLevel.IN_DANGER),

            /** BORDERLINE — Patient >30 ans 2-5 déclencheurs */
            new RiskRule(ctx -> !ctx.isYoung() && ctx.triggerCount() >= 2, RiskLevel.BORDERLINE)
    );

    /**
     * Évalue le risque en parcourant les règles par priorité.
     */
    public RiskLevel calculate(int age, Gender gender, int triggerCount) {
        if (triggerCount <= 1) {
            return RiskLevel.NONE;
        }

        var context = new RiskContext(age, gender == Gender.MALE, triggerCount);

        return RULES.stream()
                .filter(rule -> rule.condition().test(context))
                .map(RiskRule::level)
                .findFirst()
                .orElse(RiskLevel.NONE);
    }
}