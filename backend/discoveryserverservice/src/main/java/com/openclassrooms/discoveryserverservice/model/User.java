package com.openclassrooms.discoveryserverservice.model;

import lombok.*;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class User {
    private String username;
    private String password;
    private String role;
    private String authorities;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean enabled;
}
