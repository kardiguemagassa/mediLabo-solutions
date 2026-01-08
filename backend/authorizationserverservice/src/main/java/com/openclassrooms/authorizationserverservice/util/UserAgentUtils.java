package com.openclassrooms.authorizationserverservice.util;

import com.openclassrooms.authorizationserverservice.domain.Analyzer;
import jakarta.servlet.http.HttpServletRequest;

import static nl.basjes.parse.useragent.UserAgent.AGENT_NAME;
import static nl.basjes.parse.useragent.UserAgent.DEVICE_NAME;

/**
 * Utilitaires pour l'analyse du User-Agent
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

public class UserAgentUtils {
    private static final String USER_AGENT_HEADER = "user-agent";
    private static final String X_FORWARDED_FOR_HEADER = "X-FORWARDED-FOR";

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

    public static String getDevice(HttpServletRequest request){
        var uaa = Analyzer.getInstance();
        var agent = uaa.parse(request.getHeader(USER_AGENT_HEADER));
        return agent.getValue(DEVICE_NAME);
    }

    public static String getClient(HttpServletRequest request){
        var uaa = Analyzer.getInstance();
        var agent = uaa.parse(request.getHeader(USER_AGENT_HEADER));
        return agent.getValue(AGENT_NAME);
    }
}
