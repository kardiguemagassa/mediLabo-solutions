package com.openclassrooms.patientservice.repository;

import com.openclassrooms.patientservice.model.*;

import java.util.List;

/**
 * @author FirstName LastName
 * @version 1.0
 * @email magassa***REMOVED_USER***@gmail.com
 * @since 2026-05-01
 */

public interface UserRepository {
    Patient getUserByEmail(String email);
    Patient getUserByUuid(String userUuid);
    Patient getUserById(Long userId);
    Patient updateUser(String userUuid, String firstName, String lastName, String email, String phone, String bio, String address);
    String createUser(String firstName, String lastName, String email, String username, String password);
    AccountToken getAccountToken(String token);
    Patient verifyPasswordToken(String token);
    Patient enableMfa(String userUuid);
    Patient disableMfa(String userUuid);
    Patient toggleAccountExpired(String userUuid);
    Patient toggleAccountLocked(String userUuid);
    Patient toggleAccountEnabled(String userUuid);
    Patient toggleCredentialsExpired(String userUuid);
    void updatePassword(String userUuid, String encodedPassword);
    Patient updateRole(String userUuid, String role);
    void resetPassword(String email);
    void doResetPassword(String userUuid, String token, String password, String confirmPassword);
    List<Patient> getUsers();
    Patient getAssignee(String ticketUuid);
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
    List<Patient> getTechSupports();
    Patient getTicketUser(String ticketUuid);
}
