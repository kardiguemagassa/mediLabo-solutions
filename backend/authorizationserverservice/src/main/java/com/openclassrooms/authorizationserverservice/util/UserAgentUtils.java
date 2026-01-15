package com.openclassrooms.authorizationserverservice.util;

import com.openclassrooms.authorizationserverservice.domain.Analyzer;
import jakarta.servlet.http.HttpServletRequest;

import static nl.basjes.parse.useragent.UserAgent.AGENT_NAME;
import static nl.basjes.parse.useragent.UserAgent.DEVICE_NAME;

/**
 * Utilitaires pour l'analyse du User-Agent et la récupération d'informations client.
 * Fournit des méthodes pour obtenir l'adresse IP, le type d'appareil et le nom du client (navigateur ou application).
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 * @email magassakara@gmail.com
 */

public class UserAgentUtils {
    /** Nom de l'en-tête HTTP User-Agent
     * */
    private static final String USER_AGENT_HEADER = "user-agent";
    /** Nom de l'en-tête HTTP X-FORWARDED-FOR utilisé pour récupérer l'IP derrière un proxy
     * */
    private static final String X_FORWARDED_FOR_HEADER = "X-FORWARDED-FOR";

    /**
     * Récupère l'adresse IP du client depuis la requête HTTP.
     * <p>
     * Si la requête passe par un proxy, l'adresse IP réelle est extraite de l'en-tête
     * X-FORWARDED-FOR. Sinon, on utilise l'adresse distante de la requête.
     * </p>
     *
     * @param request la requête HTTP
     * @return l'adresse IP du client, ou "Unknown IP" si indisponible
     */
    public static String getIpAddress(HttpServletRequest request) {
        var ipAddress = "Unknown IP";
        if (request != null) {
            ipAddress = request.getHeader(X_FORWARDED_FOR_HEADER);
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = request.getRemoteAddr();
            }
        }
        return ipAddress;
    }

    /**
     * Récupère le type d'appareil (desktop, mobile, tablet, etc.) à partir du User-Agent.
     *
     * @param request la requête HTTP contenant l'en-tête User-Agent
     * @return le nom du type d'appareil
     */
    public static String getDevice(HttpServletRequest request){
        var uaa = Analyzer.getInstance();
        var agent = uaa.parse(request.getHeader(USER_AGENT_HEADER));
        return agent.getValue(DEVICE_NAME);
    }

    /**
     * Récupère le nom du client (navigateur ou application) à partir du User-Agent.
     *
     * @param request la requête HTTP contenant l'en-tête User-Agent
     * @return le nom du client (ex. Chrome, Firefox, Postman)
     */
    public static String getClient(HttpServletRequest request){
        var uaa = Analyzer.getInstance();
        var agent = uaa.parse(request.getHeader(USER_AGENT_HEADER));
        return agent.getValue(AGENT_NAME);
    }
}
