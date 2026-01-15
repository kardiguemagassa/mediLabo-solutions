package com.openclassrooms.authorizationserverservice.dtorequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author FirstName LastName
 * @version 1.0
 * @email magassa***REMOVED_USER***@gmail.com
 * @since 2026-05-01
 */

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasswordRequest {
    @NotEmpty(message = "Le mot de passe actuel est obligatoire.")
    private String currentPassword;
    @NotEmpty(message = "Le nouveau mot de passe est obligatoire.")
    private String newPassword;
    @NotEmpty(message = "La confirmation du mot de passe est obligatoire.")
    private String confirmNewPassword;
}
