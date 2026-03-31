/*package com.openclassrooms.userservice.security;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyUtilsTest {

    private KeyUtils keyUtils;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        keyUtils = new KeyUtils();
        ReflectionTestUtils.setField(keyUtils, "privateKey", "private.key");
        ReflectionTestUtils.setField(keyUtils, "publicKey", "public.key");
        ReflectionTestUtils.setField(keyUtils, "activeProfile", "dev");
    }

    @Test
    @DisplayName("Generate : Devrait charger les clés si elles existent déjà")
    void getRSAKeyPair_ShouldLoadExistingKeys() throws Exception {

        Path keysPath = Path.of("src/main/resources/keys");
        Files.createDirectories(keysPath);

        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        Files.write(keysPath.resolve("public.key"), new X509EncodedKeySpec(pair.getPublic().getEncoded()).getEncoded());
        Files.write(keysPath.resolve("private.key"), new PKCS8EncodedKeySpec(pair.getPrivate().getEncoded()).getEncoded());

        // WHEN
        RSAKey result = keyUtils.getRSAKeyPair();

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getKeyID()).isEqualTo("20182433-7699-40cb-b070-8174a978c556");
    }

    @Test
    @DisplayName("Generate : Devrait lever une exception en PROD si les clés manquent")
    void getRSAKeyPair_InProd_ShouldThrowException() throws Exception {
        // GIVEN
        ReflectionTestUtils.setField(keyUtils, "activeProfile", "prod");
        // On s'assure que les fichiers n'existent pas (suppression si besoin)
        Files.deleteIfExists(Path.of("src/main/resources/keys/public.key"));

        // WHEN & THEN
        assertThatThrownBy(() -> keyUtils.getRSAKeyPair())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("production");
    }

    @Test
    @DisplayName("Generate : Devrait générer de nouvelles clés si elles manquent (hors prod)")
    void getRSAKeyPair_ShouldGenerateNewKeys() throws Exception {
        // GIVEN
        ReflectionTestUtils.setField(keyUtils, "activeProfile", "dev");
        Path keysPath = Path.of("src/main/resources/keys");
        Files.deleteIfExists(keysPath.resolve("public.key"));
        Files.deleteIfExists(keysPath.resolve("private.key"));

        // WHEN
        RSAKey result = keyUtils.getRSAKeyPair();

        // THEN
        assertThat(result).isNotNull();
        assertThat(Files.exists(keysPath.resolve("public.key"))).isTrue();
        assertThat(Files.exists(keysPath.resolve("private.key"))).isTrue();
    }
}*/