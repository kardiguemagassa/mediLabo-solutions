package com.openclassrooms.userservice.dtorequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
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
public class UserRequest {
    @NotEmpty(message = "Le prénom est obligatoire.")
    private String firstName;
    @NotEmpty(message = "Le nom de famille est obligatoire.")
    private String lastName;
    @NotEmpty(message = "L'adresse e-mail est obligatoire.")
    @Email(message = "Adresse e-mail invalide")
    private String email;
    @NotEmpty(message = "Le nom d'utilisateur est obligatoire.")
    private String username;
    @NotEmpty(message = "Le mot de passe est obligatoire.")
    private String password;
    private String bio;
    private String phone;
    private String address;
}
