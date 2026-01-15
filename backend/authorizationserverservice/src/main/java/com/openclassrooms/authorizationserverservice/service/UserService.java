package com.openclassrooms.authorizationserverservice.service;

import com.openclassrooms.authorizationserverservice.dto.UserInfoDTO;
import com.openclassrooms.authorizationserverservice.model.Credential;
import com.openclassrooms.authorizationserverservice.model.Device;
import com.openclassrooms.authorizationserverservice.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service pour l'authentification des utilisateurs
 * @author FirstName LastName
 * @version 1.0
 * @email magassa***REMOVED_USER***@gmail.com
 * @since 2026-05-01
 */

public interface UserService {
    // USER MANAGEMENT TOKEN SERVICE
    User getUserByEmail(String email);
    void resetLoginAttempts(String userUuid);
    void updateLoginAttempts(String email);
    boolean verifyQrCode(String userId, String code);
    void setLastLogin(Long userId);
    void addLoginDevice(Long userId, String deviceName, String client, String ipAddress);

    // USER MANAGEMENT SERVICE
    User getUserByUuid(String userUuid);
    User updateUser(String userUuid, String firstName, String lastName, String email, String phone, String bio, String address);
    void createUser(String firstName, String lastName, String email, String username, String password);
    void verifyAccount(String token);
    User verifyPasswordToken(String token);
    User enableMfa(String userUuid);
    User disableMfa(String userUuid);
    User uploadPhoto(String userUuid, MultipartFile file);
    User toggleAccountExpired(String userUuid);
    User toggleAccountLocked(String userUuid);
    User toggleAccountEnabled(String userUuid);
    User toggleCredentialsExpired(String userUuid);
    void updatePassword(String userUuid, String currentPassword, String newPassword, String confirmNewPassword);
    User updateRole(String userUuid, String role);
    void resetPassword(String email);
    void doResetPassword(String userUuid, String token, String password, String confirmPassword);
    List<User> getUsers();
    User getAssignee(String ticketUuid);
    Credential getCredential(String userUuid);
    List<Device> getDevices(String userUuid);
    List<User> getMediLaboSupports();
    User getPatientUser(String patientUuid);

    // USER PATIENT MANAGEMENT
    UserInfoDTO getUserInfoByUuid(String userUuid);
    boolean userExistsByUuid(String userUuid);
}
