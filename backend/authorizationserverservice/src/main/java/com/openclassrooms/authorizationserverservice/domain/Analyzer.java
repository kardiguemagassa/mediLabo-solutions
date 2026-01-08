package com.openclassrooms.authorizationserverservice.domain;

import nl.basjes.parse.useragent.UserAgentAnalyzer;

/**
 * @author FirstName LastName
 * @version 1.0
 * @email magassa***REMOVED_USER***@gmail.com
 * @since 2026-05-01
 */

public class Analyzer {
    private static UserAgentAnalyzer INSTANCE;

    public static UserAgentAnalyzer getInstance() {
        if(INSTANCE == null) {
            INSTANCE = UserAgentAnalyzer
                    .newBuilder()
                    .hideMatcherLoadStats()
                    .withCache(10000)
                    .build();
        }
        return INSTANCE;
    }
}
