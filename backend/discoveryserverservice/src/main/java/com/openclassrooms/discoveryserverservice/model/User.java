package com.openclassrooms.discoveryserverservice.model;

import lombok.*;

/**
 * @author FirstName LastName
 * @version 1.0
 * @email magassa***REMOVED_USER***@gmail.com
 * @since 2026-05-01
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class User {
//    private Long userId;
//    private String userUuid;
//    private String firstName;
//    private String lastName;
    private String username;
//    private String email;
//    private String memberId;
    private String password;
//    private String phone;
//    private String bio;
//    private String imageUrl;
//    private String qrCodeImageUri;
//    private String qrCodeSecret;
//    private String lastLogin;
//    private int loginAttempts;
//    private String createdAt;
//    private String updatedAt;
    private String role;
    private String authorities;
//    private boolean mfa;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    //private boolean credentialsNonExpired;
    private boolean enabled;
}
