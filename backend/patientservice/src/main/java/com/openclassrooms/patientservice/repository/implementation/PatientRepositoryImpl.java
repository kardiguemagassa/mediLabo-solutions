package com.openclassrooms.patientservice.repository.implementation;

import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.model.*;
import com.openclassrooms.patientservice.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.openclassrooms.patientservice.utils.UserUtils.*;
import static java.lang.String.format;
import static java.sql.Types.VARCHAR;
import static java.util.Map.of;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRepositoryImpl implements PatientRepository {
    private final JdbcClient jdbc;

    @Override
    public Patient getUserByEmail(String email) {
        try {
            return jdbc.sql(SELECT_USER_BY_EMAIL_QUERY).param("email", email).query(Patient.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException(format("No user found user email %s", email));
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Patient getUserByUuid(String userUuid) {
        try {
            return jdbc.sql(SELECT_USER_BY_USER_UUID_QUERY).param("userUuid", userUuid).query(Patient.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException(format("No user found user UUID %s", userUuid));
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Patient getUserById(Long userId) {
        try {
            return jdbc.sql(SELECT_USER_BY_USER_ID_QUERY).param("userId", userId).query(Patient.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException(format("No user found user ID %s", userId));
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Patient updateUser(String userUuid, String firstName, String lastName, String email, String phone, String bio, String address) {
        try {
            return jdbc.sql(UPDATE_USER_FUNCTION).paramSource(getParamSource(userUuid, firstName, lastName, email, phone, bio, address)).query(Patient.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException(format("No user found user UUID %s", userUuid));
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public String createUser(String firstName, String lastName, String email, String username, String password) {
        try {
            var token = randomUUUID.get();
            jdbc.sql(CREATE_USER_STORED_PROCEDURE).paramSource(getParamSource(firstName, lastName, email, username, password, token)).update();
            return token;
        } catch (DuplicateKeyException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Email/username already in use. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public AccountToken getAccountToken(String token) {
        try {
            return jdbc.sql(SELECT_ACCOUNT_TOKEN_QUERY).param("token", token).query(AccountToken.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Invalid link. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Patient verifyPasswordToken(String token) {
        return null;
    }

    @Override
    public Patient enableMfa(String userUuid) {
        try {
            return jdbc.sql(ENABLE_USER_MFA_FUNCTION).paramSource(getParamSource(userUuid, qrCodeSecret.get())).query(Patient.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("User not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Patient disableMfa(String userUuid) {
        try {
            return jdbc.sql(DISABLE_USER_MFA_FUNCTION).param("userUuid", userUuid).query(Patient.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("User not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Patient toggleAccountExpired(String userUuid) {
        try {
            return jdbc.sql(TOGGLE_ACCOUNT_EXPIRED_FUNCTION).param("userUuid", userUuid).query(Patient.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("User not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Patient toggleAccountLocked(String userUuid) {
        try {
            return jdbc.sql(TOGGLE_ACCOUNT_LOCKED_FUNCTION).param("userUuid", userUuid).query(Patient.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("User not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Patient toggleAccountEnabled(String userUuid) {
        try {
            return jdbc.sql(TOGGLE_ACCOUNT_ENABLED_FUNCTION).param("userUuid", userUuid).query(Patient.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("User not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Patient toggleCredentialsExpired(String userUuid) {
        return null;
    }

    @Override
    public void updatePassword(String userUuid, String encodedPassword) {
        try {
            jdbc.sql(UPDATE_USER_PASSWORD_QUERY).params(of("userUuid", userUuid, "password", encodedPassword)).update();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("User not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Patient updateRole(String userUuid, String role) {
        try {
            return jdbc.sql(UPDATE_USER_ROLE_FUNCTION).params(of("userUuid", userUuid, "role", role)).query(Patient.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("User not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void resetPassword(String email) {

    }

    @Override
    public void doResetPassword(String userUuid, String token, String password, String confirmPassword) {

    }

    @Override
    public List<Patient> getUsers() {
        try {
            return jdbc.sql(SELECT_USERS_QUERY).query(Patient.class).list();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Users not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void deleteAccountToken(String token) {
        try {
            jdbc.sql(DELETE_ACCOUNT_TOKEN_QUERY).param("token", token).update();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Token not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void deletePasswordToken(String token) {
        try {
            jdbc.sql("").param("token", token).update();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Token not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void deletePasswordToken(Long userId) {
        try {
            jdbc.sql(DELETE_PASSWORD_TOKEN_QUERY).param("userId", userId).update();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Token not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Patient getTicketUser(String ticketUuid) {
        try {
            return jdbc.sql(SELECT_TICKET_USER_QUERY).params(of("ticketUuid", ticketUuid)).query(Patient.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException(format("No ticket found by UUID %s", ticketUuid));
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public String createPasswordToken(Long userId) {
        try {
            var token = randomUUUID.get();
            jdbc.sql(CREATE_PASSWORD_TOKEN_QUERY).params(of("userId", userId, "token", token)).update();
            return token;
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("User not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public List<Patient> getTechSupports() {
        try {
            return jdbc.sql(SELECT_TECH_SUPPORTS_QUERY).query(Patient.class).list();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Users not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public String getPassword(String userUuid) {
        try {
            return jdbc.sql(SELECT_USER_PASSWORD_QUERY).param("userUuid", userUuid).query(String.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("User not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void updateImageUrl(String userUuid, String imageUrl) {
        try {
            jdbc.sql(UPDATE_USER_IMAGE_URL_QUERY).params(of("userUuid", userUuid, "imageUrl", imageUrl)).update();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("User not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public PasswordToken getPasswordToken(Long userId) {
        try {
            return jdbc.sql(SELECT_PASSWORD_TOKEN_BY_USER_ID_QUERY).params(of("userId", userId)).query(PasswordToken.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            //throw new ApiException("Invalid link. Please try again.");
            return null;
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public PasswordToken getPasswordToken(String token) {
        try {
            return jdbc.sql(SELECT_PASSWORD_TOKEN_QUERY).param("token", token).query(PasswordToken.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Invalid link. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public void updateAccountSettings(Long userId) {
        try {
            jdbc.sql(UPDATE_ACCOUNT_SETTINGS_QUERY).param("userId", userId).update();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("User not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Patient getAssignee(String ticketUuid) {
        try {
            return jdbc.sql(SELECT_TICKET_ASSIGNEE_QUERY).param("ticketUuid", ticketUuid).query(Patient.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            log.error("Ticket is not assigned.");
            return Patient.builder().build();
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Credential getCredential(String userUuid) {
        try {
            return jdbc.sql(SELECT_USER_CREDENTIAL_QUERY).param("userUuid", userUuid).query(Credential.class).single();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("Credential not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public List<Device> getDevices(String userUuid) {
        try {
            return jdbc.sql(SELECT_DEVICES_QUERY).param("userUuid", userUuid).query(Device.class).list();
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("User not found. Please try again.");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    private SqlParameterSource getParamSource(String userUuid, String qrCodeSecret) {
        return new MapSqlParameterSource()
                .addValue("userUuid", userUuid, VARCHAR)
                .addValue("qrCodeSecret", qrCodeSecret, VARCHAR)
                .addValue("qrCodeImageUri", qrCodeImageUri.apply(qrCodeSecret), VARCHAR);
    }

    private SqlParameterSource getParamSource(String userUuid, String firstName, String lastName, String email, String phone, String bio, String address) {
        return new MapSqlParameterSource()
                .addValue("userUuid", userUuid, VARCHAR)
                .addValue("firstName", firstName, VARCHAR)
                .addValue("lastName", lastName, VARCHAR)
                .addValue("email", email.trim().toLowerCase(), VARCHAR)
                .addValue("phone", phone, VARCHAR)
                .addValue("address", address, VARCHAR)
                .addValue("bio", bio, VARCHAR);
    }

    private SqlParameterSource getParamSource(String firstName, String lastName, String email, String username, String password, String token) {
        return new MapSqlParameterSource()
                .addValue("userUuid", randomUUUID.get(), VARCHAR)
                .addValue("firstName", firstName, VARCHAR)
                .addValue("lastName", lastName, VARCHAR)
                .addValue("email", email.trim().toLowerCase(), VARCHAR)
                .addValue("username", username.trim().toLowerCase(), VARCHAR)
                .addValue("password", password, VARCHAR)
                .addValue("token", token, VARCHAR)
                .addValue("credentialUuid", randomUUUID.get(), VARCHAR)
                .addValue("memberId", memberId.get(), VARCHAR);
    }
}
