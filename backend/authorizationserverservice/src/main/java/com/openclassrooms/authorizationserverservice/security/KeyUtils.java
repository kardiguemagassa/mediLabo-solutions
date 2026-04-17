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
 * Gestionnaire des clés cryptographiques RSA utilisées pour la signature des JWT
 * Cette classe est responsable de :
 * La génération de paires de clés RSA (publique + privée)
 * Le chargement des clés depuis le système de fichiers
 * La fourniture d'un {@link RSAKey} compatible avec OAuth2 / JOSE
 * Rôle dans l'architecture OAuth2</h2>
 * Ces clés sont utilisées par :
 * Le serveur d'autorisation pour signer les JWT
 * Les Resource Servers pour vérifier les JWT
 * La clé privée signe le token, la clé publique permet de le vérifier.
 * Stratégie par environnement
 * En dev: les clés sont générées automatiquement si absentes
 * En prod: les clés doivent déjà exister (sécurité renforcée)
 * Les clés sont stockées dans le dossier : src/main/resources/keys  et sont injectées via : keys.public keys.private

 *
 * @author Kardigué MAGASSA
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
     * Cette méthode :
     * Charge les clés depuis le disque si elles existent
     * Les génère si elles n’existent pas (en environnement non-prod)
     * @return {@link RSAKey} contenant la clé publique, privée et un keyId (kid)
     */
    public RSAKey getRSAKeyPair() {
        return generateRSAKeyPair(privateKey, publicKey);
    }

    /**
     * Charge ou génère une paire de clés RSA selon l’environnement.
     * Fonctionnement :
     * Vérifie si les fichiers de clés existent
     * Si oui → charge les clés depuis le disque
     * Sinon → génère une nouvelle paire RSA (si pas en prod)
     * Stocke les clés dans le dossier {@code resources/keys}
     * En environnement prod, une erreur est levée si les clés n'existent pas,
     * afin d'éviter toute rotation accidentelle des clés (sécurité)
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
     * Si le dossier n'existe pas, il est créé automatiquement.

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
