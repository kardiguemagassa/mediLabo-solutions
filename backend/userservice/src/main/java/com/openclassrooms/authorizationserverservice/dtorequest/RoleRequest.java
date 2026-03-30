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
public class RoleRequest {
    @NotEmpty(message = "Le role est obligatoire.")
    private String role;
}