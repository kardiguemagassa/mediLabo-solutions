package com.openclassrooms.userservice.util;

import com.openclassrooms.userservice.exception.ApiException;
import com.openclassrooms.userservice.model.User;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.openclassrooms.userservice.constant.Constant.MEDI_LABO_LLC;
import static dev.samstevens.totp.util.Utils.getDataUriForImage;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;

/**
 * Utilitaires pour la gestion des utilisateurs, incluant la génération et vérification de QR codes TOTP,
 * TOTP (Time-based One-Time Password, c’est-à-dire mot de passe à usage unique basé sur le temps.)
 * la génération d'UUID et de Member IDs, ainsi que l'extraction d'utilisateurs depuis l'authentification.
 * Fournit des méthodes et lambdas pour :
 * Vérifier les codes TOTP
 * Extraire un utilisateur d'un objet Authentication
 * Générer des identifiants uniques et secrets TOTP
 * Générer des QR codes pour MFA
 *
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

public class UserUtils {

    /**
     * Fournit un UUID aléatoire sous forme de chaîne.
     * Un nouveau secret TOTP aléatoire pour chaque utilisateur.
     * Puis, qrCodeImageUri crée un QR code à scanner pour l'application connaisse ce secret.
     */
    public static Supplier<String> randomUUUID = () -> UUID.randomUUID().toString();

    /** Fournit un Member ID aléatoire au format ####-##-####.*/
    public static Supplier<String> memberId = () -> randomNumeric(4) + "-" + randomNumeric(2) + "-" + randomNumeric(4);

    /**
     * Génère un objet {@link QrData} à partir d'un secret TOTP.
     * Utilisé pour créer le QR code que l'utilisateur peut scanner.
     */
    public static Function<String, QrData> qrDataFunction = qrCodeSecret -> new QrData.Builder()
            .issuer(MEDI_LABO_LLC)
            .label(MEDI_LABO_LLC)
            .secret(qrCodeSecret)
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();

    /**
     * Génère l'URI de l'image QR code (au format Base64) à partir d'un secret TOTP.
     */
    public static Function<String, String> qrCodeImageUri = qrCodeSecret -> {
        try {
            var data = qrDataFunction.apply(qrCodeSecret);
            var generator = new ZxingPngQrGenerator();
            var imageData = generator.generate(data);
            return getDataUriForImage(imageData, generator.getImageMimeType());
        } catch (QrGenerationException exception) {
            throw new ApiException(exception.getMessage());
        }
    };

    /**
     * Fournit un secret TOTP aléatoire.
     * Utilisé pour configurer le MFA (authentification à deux facteurs).
     */
    public static Supplier<String> qrCodeSecret = () -> new DefaultSecretGenerator().generate();
}
