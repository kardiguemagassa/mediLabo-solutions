package com.openclassrooms.authorizationserverservice.security;

import com.nimbusds.jose.jwk.RSAKey;
import com.openclassrooms.authorizationserverservice.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

/**
 * Gestionnaire des clés cryptographiques RSA utilisées pour la signature des JWT.
 * <p>
 * Cette classe est responsable de :
 * <ul>
 *   <li>La génération de paires de clés RSA (publique + privée)</li>
 *   <li>Le chargement des clés depuis le système de fichiers</li>
 *   <li>La fourniture d'un {@link RSAKey} compatible avec OAuth2 / JOSE</li>
 * </ul>
 *
 * <h2>Rôle dans l’architecture OAuth2</h2>
 * <p>
 * Ces clés sont utilisées par :
 * </p>
 * <ul>
 *   <li>Le serveur d'autorisation pour <b>signer</b> les JWT</li>
 *   <li>Les Resource Servers pour <b>vérifier</b> les JWT</li>
 * </ul>
 *
 * <p>
 * La clé privée signe le token, la clé publique permet de le vérifier.
 * </p>
 *
 * <h2>Stratégie par environnement</h2>
 * <ul>
 *   <li>En <b>dev</b> : les clés sont générées automatiquement si absentes</li>
 *   <li>En <b>prod</b> : les clés doivent déjà exister (sécurité renforcée)</li>
 * </ul>
 *
 * <p>
 * Les clés sont stockées dans le dossier :
 * </p>
 * <pre>
 * src/main/resources/keys
 * </pre>
 *
 * et sont injectées via :
 * <pre>
 * keys.public
 * keys.private
 * </pre>
 *
 * @author FirstName LastName
 * @version 1.0
 * @since 2026-05-01
 */

@Component
@Slf4j
public class KeyUtils {
    private static final String RSA = "RSA";
    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Value("${keys.private}")
    private String privateKey;

    @Value("${keys.public}")
    private String publicKey;

    /**
     * Retourne la paire de clés RSA utilisée pour la signature et la validation des JWT.
     *
     * <p>
     * Cette méthode :
     * <ul>
     *   <li>Charge les clés depuis le disque si elles existent</li>
     *   <li>Les génère si elles n’existent pas (en environnement non-prod)</li>
     * </ul>
     *
     * @return {@link RSAKey} contenant la clé publique, privée et un keyId (kid)
     */
    public RSAKey getRSAKeyPair() {
        return generateRSAKeyPair(privateKey, publicKey);
    }

    // GENERATE KEY IN LOCAL ENVIRONMENT NOT PROD ENVIRONMENT

    /**
     * Charge ou génère une paire de clés RSA selon l’environnement.
     *
     * <p>
     * Fonctionnement :
     * </p>
     * <ol>
     *   <li>Vérifie si les fichiers de clés existent</li>
     *   <li>Si oui → charge les clés depuis le disque</li>
     *   <li>Sinon → génère une nouvelle paire RSA (si pas en prod)</li>
     *   <li>Stocke les clés dans le dossier {@code resources/keys}</li>
     * </ol>
     *
     * <p>
     * En environnement <b>prod</b>, une erreur est levée si les clés n'existent pas,
     * afin d'éviter toute rotation accidentelle des clés (sécurité).
     * </p>
     *
     * @param privateKeyName nom du fichier de la clé privée
     * @param publicKeyName nom du fichier de la clé publique
     * @return {@link RSAKey} utilisable par Spring Authorization Server
     */
    private RSAKey generateRSAKeyPair(String privateKeyName, String publicKeyName) {
        KeyPair keyPair;
        var keysDirectory = Paths.get("src","main","resources", "keys");
        verifyKeysDirectory(keysDirectory);
        if(Files.exists(keysDirectory.resolve(publicKeyName))  && Files.exists(keysDirectory.resolve(publicKeyName))) {
            log.info("RSA keys already exist. Loading keys from file paths: {}, {}", publicKeyName, privateKeyName);
            var privateKeyFile = keysDirectory.resolve(privateKeyName).toFile();
            var publicKeyFile = keysDirectory.resolve(publicKeyName).toFile();
            try {
                var keyFactory = KeyFactory.getInstance(RSA);
                byte[] publicKeyBytes = Files.readAllBytes(publicKeyFile.toPath());
                EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
                RSAPublicKey  publicKey = (RSAPublicKey)keyFactory.generatePublic(publicKeySpec);

                byte[] privateKeyBytes = Files.readAllBytes(privateKeyFile.toPath());
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
                RSAPrivateKey privateKey = (RSAPrivateKey)keyFactory.generatePrivate(privateKeySpec);

                var keyId = "20182433-7699-40cb-b070-8174a978c556"; //UUID.randomUUID().toString();
                log.info("Key ID: {}", keyId);
                return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(keyId).build();
            } catch (Exception exception) {
                log.error(exception.getMessage());
                throw new RuntimeException(exception);
            }
        } else {
            if (activeProfile.equalsIgnoreCase("prod")) {
                throw new RuntimeException("Les clés publiques et privées n'existent pas dans l'environnement de production.");
            }
        }
        try {
            log.info("Generating new public and private keys: {}, {}", publicKeyName, privateKeyName);
            var keyPairGenerator = KeyPairGenerator.getInstance(RSA);
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            try (var fos = new FileOutputStream(keysDirectory.resolve(publicKeyName).toFile())) {
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());
                fos.write(keySpec.getEncoded());
            }
            try (var fos = new FileOutputStream(keysDirectory.resolve(privateKeyName).toFile())) {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded());
                fos.write(keySpec.getEncoded());
            }
            return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(UUID.randomUUID().toString()).build();
        } catch (Exception exception) {
            throw new ApiException(exception.getMessage());
        }
    }

    /**
     * Vérifie l'existence du dossier de stockage des clés RSA.
     *
     * <p>
     * Si le dossier n'existe pas, il est créé automatiquement.
     * </p>
     *
     * @param keysDirectory chemin du dossier de clés
     */
    private static void verifyKeysDirectory(Path keysDirectory) {
        if(!Files.exists(keysDirectory)) {
            try {
                Files.createDirectories(keysDirectory);
            } catch (Exception exception) {
                throw new ApiException(exception.getMessage());
            }
            log.info("Created keys directory: {}", keysDirectory);
        }
    }
}
