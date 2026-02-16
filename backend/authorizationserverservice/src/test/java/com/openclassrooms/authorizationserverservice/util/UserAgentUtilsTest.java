package com.openclassrooms.authorizationserverservice.util;

import com.openclassrooms.authorizationserverservice.domain.Analyzer;
import jakarta.servlet.http.HttpServletRequest;

import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAgentUtilsTest {

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("getIpAddress doit retourner l'IP du header X-Forwarded-For si présent")
    void getIpAddress_ShouldReturnXForwardedFor() {
        // GIVEN
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("192.168.1.1");

        // WHEN
        String ip = UserAgentUtils.getIpAddress(request);

        // THEN
        assertThat(ip).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("getIpAddress doit retourner l'IP du header X-Forwarded-For si non vide")
    void getIpAddress_ShouldReturnXForwardedForWhenNotEmpty() {
        // Couvre la branche ipAddress.isEmpty()
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String ip = UserAgentUtils.getIpAddress(request);

        assertThat(ip).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("getIpAddress doit retourner RemoteAddr si le header est absent")
    void getIpAddress_ShouldReturnRemoteAddr() {
        // GIVEN
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");

        // WHEN
        String ip = UserAgentUtils.getIpAddress(request);

        // THEN
        assertThat(ip).isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("getIpAddress doit retourner Unknown IP si la requête est null")
    void getIpAddress_ShouldReturnUnknownIfRequestNull() {
        assertThat(UserAgentUtils.getIpAddress(null)).isEqualTo("Unknown IP");
    }

    @Test
    @DisplayName("getInstance doit initialiser le singleton")
    void getInstance_ShouldInitializeOnlyOnce() throws Exception {
        // Réinitialisation de l'instance via réflexion (pour JaCoCo)
        java.lang.reflect.Field field = Analyzer.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        field.set(null, null);

        // Premier appel (déclenche le build)
        // ATTENTION : Ce test risque de nécessiter Kryo dans le pom.xml
        // car il exécute le VRAI code de la librairie.
        UserAgentAnalyzer instance1 = Analyzer.getInstance();
        UserAgentAnalyzer instance2 = Analyzer.getInstance();

        assertThat(instance1).isNotNull();
        assertThat(instance1).isSameAs(instance2);
    }

    @Test
    @DisplayName("Utils : Devrait couvrir getDevice et getClient")
    void shouldCoverGetDeviceAndGetClient() {
        try (MockedStatic<Analyzer> mockedAnalyzer = mockStatic(Analyzer.class)) {
            // 1. On mocke l'analyseur
            UserAgentAnalyzer mockUaa = mock(UserAgentAnalyzer.class);

            // 2.  On mocke la classe concrète attendue par Yauaa cela évite l'erreur WrongTypeOfReturnValue
            nl.basjes.parse.useragent.UserAgent.ImmutableUserAgent mockAgent =
                    mock(nl.basjes.parse.useragent.UserAgent.ImmutableUserAgent.class);

            // 3. Configuration du Singleton
            mockedAnalyzer.when(Analyzer::getInstance).thenReturn(mockUaa);

            // 4. Mock de la requête
            when(request.getHeader("user-agent")).thenReturn("Mozilla/5.0...");

            // 5. On branche le mock concrétement
            doReturn(mockAgent).when(mockUaa).parse(anyString());

            // 6. Simulation des valeurs
            when(mockAgent.getValue("DeviceName")).thenReturn("MyDevice");
            when(mockAgent.getValue("AgentName")).thenReturn("MyBrowser");

            // WHEN
            String device = UserAgentUtils.getDevice(request);
            String client = UserAgentUtils.getClient(request);

            // THEN
            assertThat(device).isEqualTo("MyDevice");
            assertThat(client).isEqualTo("MyBrowser");
        }
    }
}