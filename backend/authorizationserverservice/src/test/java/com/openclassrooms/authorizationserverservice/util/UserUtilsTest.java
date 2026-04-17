package com.openclassrooms.authorizationserverservice.util;

import com.openclassrooms.authorizationserverservice.model.User;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;

import java.util.UUID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserUtilsTest {

    @Test
    @DisplayName("verifyCode doit retourner false si le secret est null ou vide")
    void verifyCode_ShouldReturnFalseForInvalidSecret() {
        assertThat(UserUtils.verifyCode(null, "123456")).isFalse();
        assertThat(UserUtils.verifyCode("", "123456")).isFalse();
    }

    @Test
    @DisplayName("verifyCode doit valider un code correct")
    void verifyCode_ShouldVerifyCorrectCode() throws Exception {
        // GIVEN
        String secret = new DefaultSecretGenerator().generate();
        long counter = System.currentTimeMillis() / 1000 / 30;
        CodeGenerator codeGenerator = new DefaultCodeGenerator();

        // On génère le code attendu
        String currentCode = codeGenerator.generate(secret, counter);

        // WHEN
        boolean isValid = UserUtils.verifyCode(secret, currentCode);

        // THEN
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("getUser doit extraire l'utilisateur d'un OAuth2AuthorizationCodeRequestAuthenticationToken")
    void getUser_ShouldHandleOAuth2Token() {
        // GIVEN
        User mockUser = new User();
        mockUser.setFirstName("testUser");

        // On mock la structure imbriquée : Auth -> Principal (qui est une autre Auth) -> Principal (User)
        UsernamePasswordAuthenticationToken innerAuth = mock(UsernamePasswordAuthenticationToken.class);
        when(innerAuth.getPrincipal()).thenReturn(mockUser);

        OAuth2AuthorizationCodeRequestAuthenticationToken oauthToken = mock(OAuth2AuthorizationCodeRequestAuthenticationToken.class);
        when(oauthToken.getPrincipal()).thenReturn(innerAuth);

        // WHEN
        User result = UserUtils.getUser(oauthToken);

        // THEN
        assertThat(result).isEqualTo(mockUser);
    }

    @Test
    @DisplayName("getUser doit extraire l'utilisateur d'une authentification classique")
    void getUser_ShouldHandleStandardAuthentication() {
        // GIVEN
        User mockUser = new User();
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(mockUser);

        // WHEN
        User result = UserUtils.getUser(auth);

        // THEN
        assertThat(result).isEqualTo(mockUser);
    }

    @Test
    @DisplayName("getUser doit extraire l'utilisateur d'un jeton OAuth2")
    void getUser_ShouldHandleOAuth2Instance() {
        // GIVEN
        User mockUser = mock(User.class);

        // Le Principal du token OAuth2 doit être un UsernamePasswordAuthenticationToken
        UsernamePasswordAuthenticationToken innerAuth = mock(UsernamePasswordAuthenticationToken.class);
        when(innerAuth.getPrincipal()).thenReturn(mockUser);

        // Le token principal
        OAuth2AuthorizationCodeRequestAuthenticationToken oauthToken = mock(OAuth2AuthorizationCodeRequestAuthenticationToken.class);
        when(oauthToken.getPrincipal()).thenReturn(innerAuth);

        // WHEN
        User result = UserUtils.getUser(oauthToken);

        // THEN
        assertThat(result).isEqualTo(mockUser);
    }

//    @Test
//    @DisplayName("randomUUUID doit fournir un UUID valide")
//    void randomUUUID_ShouldReturnValidUUID() {
//        String uuid = UserUtils.randomUUUID.get();
//        assertThat(uuid).isNotBlank();
//        assertThat(UUID.fromString(uuid)).isNotNull(); // Vérifie que c'est un format UUID réel
//    }
//
//    @Test
//    @DisplayName("memberId doit fournir un ID au format ####-##-####")
//    void memberId_ShouldReturnFormattedId() {
//        String id = UserUtils.memberId.get();
//        // Vérifie le format via Regex : 4 chiffres, un tiret, 2 chiffres, un tiret, 4 chiffres
//        assertThat(id).matches("^\\d{4}-\\d{2}-\\d{4}$");
//    }
//
//    @Test
//    @DisplayName("qrCodeSecret doit générer un secret non vide")
//    void qrCodeSecret_ShouldGenerateSecret() {
//        String secret = UserUtils.qrCodeSecret.get();
//        assertThat(secret).isNotBlank();
//        assertThat(secret.length()).isGreaterThan(10);
//    }

}
