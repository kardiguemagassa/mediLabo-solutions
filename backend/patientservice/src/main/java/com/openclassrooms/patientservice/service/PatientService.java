package com.openclassrooms.patientservice.service;

import com.openclassrooms.patientservice.model.Patient;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author FirstName LastName
 * @version 1.0
 * @email magassa***REMOVED_USER***@gmail.com
 * @since 2026-05-01
 */

public interface UserService {
    Patient getUserByEmail(String email);
    Patient getUserByUuid(String userUuid);
    Patient updateUser(String userUuid, String firstName, String lastName, String email, String phone, String bio, String address);
    void createUser(String firstName, String lastName, String email, String username, String password);
    void verifyAccount(String token);
    Patient verifyPasswordToken(String token);
    Patient enableMfa(String userUuid);
    Patient disableMfa(String userUuid);
    Patient uploadPhoto(String userUuid, MultipartFile file);
    Patient toggleAccountExpired(String userUuid);
    Patient toggleAccountLocked(String userUuid);
    Patient toggleAccountEnabled(String userUuid);
    Patient toggleCredentialsExpired(String userUuid);
    void updatePassword(String userUuid, String currentPassword, String newPassword, String confirmNewPassword);
    Patient updateRole(String userUuid, String role);
    void resetPassword(String email);
    void doResetPassword(String userUuid, String token, String password, String confirmPassword);
    List<Patient> getUsers();
    Patient getAssignee(String ticketUuid);
    Credential getCredential(String userUuid);
    List<Device> getDevices(String userUuid);
    List<Patient> getTechSupports();
    Patient getTicketUser(String ticketUuid);
}
