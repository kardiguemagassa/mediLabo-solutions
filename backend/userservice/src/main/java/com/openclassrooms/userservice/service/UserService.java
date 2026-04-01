package com.openclassrooms.userservice.service;

//import com.openclassrooms.userservice.dto.UserInfoDTO;
import com.openclassrooms.userservice.model.Credential;
import com.openclassrooms.userservice.model.Device;
import com.openclassrooms.userservice.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service pour l'authentification des utilisateurs
 * @author Kardigué MAGASSA
 * @version 1.0
 * @since 2026-05-01
 */

public interface UserService {
    User getUserByEmail(String email);
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
    User getAssignee(String patientUuid);
    Credential getCredential(String userUuid);
    List<Device> getDevices(String userUuid);
    List<User> getMediLaboSupports();
    User getPatientUser(String patientUuid);

    // USER PATIENT MANAGEMENT
    //UserInfoDTO getUserInfoByUuid(String userUuid);
    boolean userExistsByUuid(String userUuid);
}
