//package com.openclassrooms.authorizationserverservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class RegisterRequest {
//
//    @NotBlank(message = "Le prénom est requis")
//    @Size(min = 2, max = 25, message = "Le prénom doit contenir entre 2 et 25 caractères")
//    private String firstName;
//
//    @NotBlank(message = "Le nom est requis")
//    @Size(min = 2, max = 25, message = "Le nom doit contenir entre 2 et 25 caractères")
//    private String lastName;
//
//    @NotBlank(message = "L'email est requis")
//    @Email(message = "Format d'email invalide")
//    private String email;
//
//    @NotBlank(message = "Le mot de passe est requis")
//    @Size(min = 8, max = 100, message = "Le mot de passe doit contenir au moins 8 caractères")
//    private String password;
//
//    private String phone;
//}
