package com.openclassrooms.userservice.repository;

import com.openclassrooms.userservice.model.*;

import java.util.List;

/**
 * Repository pour les opérations utilisateur (authentification)
 * @author FirstName LastName
 * @version 1.0
 * @email magassa***REMOVED_USER***@gmail.com
 * @since 2026-05-01
 */

public interface UserRepository {
    // USER MANAGEMENT TOKEN REPOSITORY
    //User getUserByUuid(String userId);
    User getUserByEmail(String email);
    void resetLoginAttempts(String userUuid);
    void updateLoginAttempts(String email);
    void setLastLogin(Long userId);
    void addLoginDevice(Long userId, String device, String client, String ipAddress);

    // USER MANAGEMENT REPOSITORY
    User getUserByUuid(String userUuid);
    User getUserById(Long userId);
    User updateUser(String userUuid, String firstName, String lastName, String email, String phone, String bio, String address);
    String createUser(String firstName, String lastName, String email, String username, String password);
    AccountToken getAccountToken(String token);
    User verifyPasswordToken(String token);
    User enableMfa(String userUuid);
    User disableMfa(String userUuid);
    User toggleAccountExpired(String userUuid);
    User toggleAccountLocked(String userUuid);
    User toggleAccountEnabled(String userUuid);
    User toggleCredentialsExpired(String userUuid);
    void updatePassword(String userUuid, String encodedPassword);
    User updateRole(String userUuid, String role);
    void resetPassword(String email);
    void doResetPassword(String userUuid, String token, String password, String confirmPassword);
    List<User> getUsers();
    User getAssignee(String ticketUuid);
    Credential getCredential(String userUuid);
    List<Device> getDevices(String userUuid);
    void deleteAccountToken(String token);
    void updateAccountSettings(Long userId);
    PasswordToken getPasswordToken(String token);
    PasswordToken getPasswordToken(Long userId);
    void deletePasswordToken(String token);
    void deletePasswordToken(Long userId);
    void updateImageUrl(String userUuid, String imageUrl);
    String getPassword(String userUuid);
    String createPasswordToken(Long userId);
    List<User> getMediLaboSupports();
    User getPatientUser(String ticketUuid);
}
