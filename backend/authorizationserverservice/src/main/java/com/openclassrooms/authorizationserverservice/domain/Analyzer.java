package com.openclassrooms.authorizationserverservice.domain;

import nl.basjes.parse.useragent.UserAgentAnalyzer;

/**
 * Fournit un singleton de {@link UserAgentAnalyzer} pour analyser les User Agents
 * des requêtes HTTP. Cette classe permet de détecter :
 * Le navigateur utilisé par le client
 * Le système d'exploitation du client
 * Le type d'appareil (mobile, tablette, desktop, etc.)
 * En utilisant un singleton, l'analyseur n'est construit qu'une seule fois, ce qui :
 * Améliore les performances en évitant la reconstruction à chaque requête
 * Permet de mettre en cache jusqu'à 10 000 User Agents pour accélérer l'analyse
 * Masque les logs de chargement des règles pour garder les logs propres
 * Exemple d'utilisation :
 * UserAgentAnalyzer analyzer = Analyzer.getInstance();
 * var result = analyzer.parse("Mozilla/5.0 (Windows NT 10.0; Win64; x64) ...");
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 * @see UserAgentAnalyzer
 */

public class Analyzer {

    /**
     * Instance singleton de {@link UserAgentAnalyzer}.
     * Stocker cette instance statique permet de réutiliser le même analyseur
     * pour toutes les requêtes et d'optimiser la performance.
     */
    private static UserAgentAnalyzer INSTANCE;


    /**
     * Retourne l'instance unique (singleton) de {@link UserAgentAnalyzer}.
     * Si l'instance n'existe pas encore, elle est créée avec :
     * {@code hideMatcherLoadStats()} : cache les logs de chargement des règles
     *   {@code withCache(10000)} : met en cache jusqu'à 10 000 User Agents
     * @return UserAgentAnalyzer singleton
     */
    public static UserAgentAnalyzer getInstance() {
        if(INSTANCE == null) {
            INSTANCE = UserAgentAnalyzer
                    .newBuilder()
                    .hideMatcherLoadStats() // désactive l'affichage des stats de chargement
                    .withCache(10000) // met en cache les 10000 derniers User Agents analysés
                    .build();
        }
        return INSTANCE;
    }
}
