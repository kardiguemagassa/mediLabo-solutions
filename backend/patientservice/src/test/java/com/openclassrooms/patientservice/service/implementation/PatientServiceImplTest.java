package com.openclassrooms.patientservice.service.implementation;

import com.openclassrooms.patientservice.dto.PatientRequestDTO;
import com.openclassrooms.patientservice.dto.UserRequestDTO;
import com.openclassrooms.patientservice.dto.PatientResponseDTO;
import com.openclassrooms.patientservice.event.Event;
import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.mapper.PatientMapper;
import com.openclassrooms.patientservice.model.Patient;
import com.openclassrooms.patientservice.repository.PatientRepository;
import com.openclassrooms.patientservice.service.UserServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.openclassrooms.patientservice.enumeration.EventType.PATIENT_DELETED;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private PatientMapper patientMapper;
    @Mock
    private UserServiceClient userService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PatientServiceImpl patientService;

    private Patient patient;
    private PatientResponseDTO patientResponseDTO;
    private UserRequestDTO userRequestDTO;

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .patientId(1L)
                .patientUuid("patient-uuid-001")
                .userUuid("user-uuid-001")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("M")
                .bloodType("A+")
                .heightCm(180)
                .weightKg(BigDecimal.valueOf(75.5))
                .medicalRecordNumber("MED-2026-000001")
                .active(true)
                .build();

        patientResponseDTO = PatientResponseDTO.builder()
                .patientUuid("patient-uuid-001")
                .medicalRecordNumber("MED-2026-000001")
                .build();

        userRequestDTO = new UserRequestDTO();
        userRequestDTO.setUserUuid("user-uuid-001");
        userRequestDTO.setFirstName("John");
        userRequestDTO.setLastName("Doe");
        userRequestDTO.setEmail("john@test.com");
    }

    // CREATE

    @Nested
    @DisplayName("getAllPatientsPageable - Tests")
    class GetAllPatientsPageableTests {

        @Test
        @DisplayName("getAllPatientsPageable - retourne une page vide")
        void getAllPatientsPageable_ShouldReturnEmptyPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Patient> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(patientRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(emptyPage);

            // When & Then
            StepVerifier.create(patientService.getAllPatientsPageable(pageable))
                    .expectNextMatches(page -> page.getContent().isEmpty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("getAllPatientsPageable - retourne une page avec patients enrichis")
        void getAllPatientsPageable_ShouldReturnEnrichedPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Patient> patientPage = new PageImpl<>(List.of(patient), pageable, 1);

            when(patientRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(patientPage);
            when(userService.getUserByUuid("user-uuid-001")).thenReturn(Mono.just(userRequestDTO));
            when(patientMapper.toResponseWithUserInfo(patient, userRequestDTO)).thenReturn(patientResponseDTO);

            // When & Then
            StepVerifier.create(patientService.getAllPatientsPageable(pageable))
                    .expectNextMatches(page ->
                            page.getTotalElements() == 1 &&
                                    page.getContent().getFirst().equals(patientResponseDTO))
                    .verifyComplete();
        }

        @Test
        @DisplayName("getAllPatientsPageable - erreur enrichissement mais continue avec DTO simple")
        void getAllPatientsPageable_ShouldFallbackToSimpleDtoWhenEnrichmentFails() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Patient> patientPage = new PageImpl<>(List.of(patient), pageable, 1);

            when(patientRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(patientPage);
            when(userService.getUserByUuid("user-uuid-001"))
                    .thenReturn(Mono.error(new RuntimeException("Service indisponible")));
            when(patientMapper.toResponse(patient)).thenReturn(patientResponseDTO);

            // When & Then
            StepVerifier.create(patientService.getAllPatientsPageable(pageable))
                    .expectNextMatches(page ->
                            page.getTotalElements() == 1 &&
                                    page.getContent().getFirst().equals(patientResponseDTO))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("deletePatient - Tests supplémentaires")
    class DeletePatientAdditionalTests {

        @Test
        @DisplayName("deletePatient - erreur si patient déjà inactif")
        void deletePatient_ShouldFailIfAlreadyInactive() {
            // Given
            Patient inactivePatient = patient.toBuilder().active(false).build();

            when(patientRepository.findByPatientUuid("patient-uuid-001")).thenReturn(Optional.of(inactivePatient));

            // When & Then
            StepVerifier.create(patientService.deletePatient("patient-uuid-001"))
                    .expectErrorMatches(throwable -> throwable instanceof ApiException
                            && throwable.getMessage().equals("Ce patient est déjà désactivé"))
                    .verify();

            verify(patientRepository, never()).softDeleteByPatientUuid(anyString());
        }

        @Test
        @DisplayName("deletePatient - erreur si soft delete retourne 0")
        void deletePatient_ShouldFailIfSoftDeleteReturnsZero() {
            // Given
            when(patientRepository.findByPatientUuid("patient-uuid-001")).thenReturn(Optional.of(patient));
            when(userService.getUserByUuid("user-uuid-001")).thenReturn(Mono.just(userRequestDTO));
            when(patientRepository.softDeleteByPatientUuid("patient-uuid-001")).thenReturn(0);

            // When & Then
            StepVerifier.create(patientService.deletePatient("patient-uuid-001"))
                    .expectErrorMatches(throwable -> throwable instanceof ApiException
                            && throwable.getMessage().equals("Erreur lors de la suppression du patient"))
                    .verify();
        }
    }

    @Test
    @DisplayName("createPatient - succès")
    void createPatient_ShouldReturnResponse() {
        PatientRequestDTO request = new PatientRequestDTO();
        request.setUserUuid("user-uuid-001");
        request.setDateOfBirth(LocalDate.of(1990, 5, 15));

        when(patientRepository.existsByUserUuid("user-uuid-001")).thenReturn(false);
        when(userService.getUserByUuid("user-uuid-001")).thenReturn(Mono.just(userRequestDTO));
        when(patientMapper.toEntity(any(PatientRequestDTO.class), anyString())).thenReturn(patient);
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);
        when(patientRepository.existsByMedicalRecordNumber(anyString())).thenReturn(false);
        when(patientMapper.toResponse(patient)).thenReturn(patientResponseDTO);

        StepVerifier.create(patientService.createPatient(request))
                .expectNext(patientResponseDTO)
                .verifyComplete();

        verify(patientRepository).save(any(Patient.class));
        // Correction : utiliser ArgumentCapther ou un matcher plus spécifique
        verify(eventPublisher, times(1)).publishEvent(any(Event.class));
    }

    @Test
    @DisplayName("createPatient - erreur si dossier existe déjà")
    void createPatient_ShouldFailIfAlreadyExists() {
        PatientRequestDTO request = new PatientRequestDTO();
        request.setUserUuid("user-uuid-001");

        when(patientRepository.existsByUserUuid("user-uuid-001")).thenReturn(true);

        StepVerifier.create(patientService.createPatient(request))
                .expectError(ApiException.class)
                .verify();

        verify(patientRepository, never()).save(any());
    }

    // READ

    @Test
    @DisplayName("getPatientByUuid - succès")
    void getPatientByUuid_ShouldReturnResponse() {
        when(patientRepository.findByPatientUuid("patient-uuid-001")).thenReturn(Optional.of(patient));
        when(userService.getUserByUuid("user-uuid-001")).thenReturn(Mono.just(userRequestDTO));
        when(patientMapper.toResponseWithUserInfo(patient, userRequestDTO)).thenReturn(patientResponseDTO);

        StepVerifier.create(patientService.getPatientByUuid("patient-uuid-001"))
                .expectNext(patientResponseDTO)
                .verifyComplete();
    }

    @Test
    @DisplayName("getPatientByUuid - erreur si non trouvé")
    void getPatientByUuid_ShouldFailIfNotFound() {
        when(patientRepository.findByPatientUuid("inexistant")).thenReturn(Optional.empty());

        StepVerifier.create(patientService.getPatientByUuid("inexistant"))
                .expectError(ApiException.class)
                .verify();
    }

    @Test
    @DisplayName("getPatientByUserUuid - succès")
    void getPatientByUserUuid_ShouldReturnResponse() {
        when(patientRepository.findByUserUuid("user-uuid-001")).thenReturn(Optional.of(patient));
        when(userService.getUserByUuid("user-uuid-001")).thenReturn(Mono.just(userRequestDTO));
        when(patientMapper.toResponseWithUserInfo(patient, userRequestDTO)).thenReturn(patientResponseDTO);

        StepVerifier.create(patientService.getPatientByUserUuid("user-uuid-001"))
                .expectNext(patientResponseDTO)
                .verifyComplete();
    }

    @Test
    @DisplayName("getAllActivePatients - retourne la liste enrichie")
    void getAllActivePatients_ShouldReturnEnrichedList() {
        when(patientRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(patient));
        when(userService.getUserByUuid("user-uuid-001")).thenReturn(Mono.just(userRequestDTO));
        when(patientMapper.toResponseWithUserInfo(patient, userRequestDTO)).thenReturn(patientResponseDTO);

        StepVerifier.create(patientService.getAllActivePatients())
                .expectNext(patientResponseDTO)
                .verifyComplete();
    }

    @Test
    @DisplayName("getAllActivePatientsPageable - retourne une page")
    void getAllActivePatientsPageable_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> page = new PageImpl<>(List.of(patient), pageable, 1);

        when(patientRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);
        when(patientMapper.toResponse(patient)).thenReturn(patientResponseDTO);

        StepVerifier.create(patientService.getAllActivePatientsPageable(pageable))
                .expectNextMatches(p -> p.getTotalElements() == 1
                        && p.getContent().getFirst().equals(patientResponseDTO))
                .verifyComplete();
    }

    @Test
    @DisplayName("updatePatient - succès avec save JPA")
    void updatePatient_ShouldReturnUpdated() {
        PatientRequestDTO request = new PatientRequestDTO();
        request.setDateOfBirth(LocalDate.of(1990, 5, 15));

        Patient updatedPatient = patient.toBuilder().bloodType("B+").build();

        when(patientRepository.findByPatientUuid("patient-uuid-001")).thenReturn(Optional.of(patient));
        when(userService.getUserByUuid("user-uuid-001")).thenReturn(Mono.just(userRequestDTO));
        when(patientMapper.updateEntity(patient, request)).thenReturn(updatedPatient);
        when(patientRepository.save(updatedPatient)).thenReturn(updatedPatient);
        when(patientMapper.toResponseWithUserInfo(updatedPatient, userRequestDTO)).thenReturn(patientResponseDTO);

        StepVerifier.create(patientService.updatePatient("patient-uuid-001", request))
                .expectNext(patientResponseDTO)
                .verifyComplete();

        verify(patientRepository).save(updatedPatient);
    }

    @Test
    @DisplayName("deletePatient - soft delete avec int retour")
    void deletePatient_ShouldSoftDelete() {
        when(patientRepository.findByPatientUuid("patient-uuid-001")).thenReturn(Optional.of(patient));
        when(userService.getUserByUuid("user-uuid-001")).thenReturn(Mono.just(userRequestDTO));
        when(patientRepository.softDeleteByPatientUuid("patient-uuid-001")).thenReturn(1);

        StepVerifier.create(patientService.deletePatient("patient-uuid-001"))
                .verifyComplete();

        verify(patientRepository).softDeleteByPatientUuid("patient-uuid-001");

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        Event capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo(PATIENT_DELETED);
    }

    @Test
    @DisplayName("deletePatient - erreur si patient non trouvé")
    void deletePatient_ShouldFailIfNotFound() {
        when(patientRepository.findByPatientUuid("inexistant")).thenReturn(Optional.empty());

        StepVerifier.create(patientService.deletePatient("inexistant"))
                .expectError(ApiException.class)
                .verify();
    }

    @Test
    @DisplayName("restorePatient - succès avec int retour")
    void restorePatient_ShouldReactivate() {
        Patient deletedPatient = patient.toBuilder().active(false).build();

        when(patientRepository.findByPatientUuidAndActiveFalse("patient-uuid-001")).thenReturn(Optional.of(deletedPatient));
        when(patientRepository.restoreByPatientUuid("patient-uuid-001")).thenReturn(1);
        when(userService.getUserByUuid("user-uuid-001")).thenReturn(Mono.just(userRequestDTO));
        when(patientMapper.toResponseWithUserInfo(deletedPatient, userRequestDTO)).thenReturn(patientResponseDTO);

        StepVerifier.create(patientService.restorePatient("patient-uuid-001"))
                .expectNext(patientResponseDTO)
                .verifyComplete();

        verify(patientRepository).restoreByPatientUuid("patient-uuid-001");
    }

    @Test
    @DisplayName("restorePatient - erreur si restore échoue")
    void restorePatient_ShouldFailIfRestoreReturnsZero() {
        when(patientRepository.findByPatientUuidAndActiveFalse("patient-uuid-001")).thenReturn(Optional.of(patient));
        when(patientRepository.restoreByPatientUuid("patient-uuid-001")).thenReturn(0);

        StepVerifier.create(patientService.restorePatient("patient-uuid-001"))
                .expectError(ApiException.class)
                .verify();
    }

    @Test
    @DisplayName("getPatientByMedicalRecordNumber - succès")
    void getByMedicalRecord_ShouldReturnResponse() {
        when(patientRepository.findByMedicalRecordNumber("MED-2026-000001")).thenReturn(Optional.of(patient));
        when(userService.getUserByUuid("user-uuid-001")).thenReturn(Mono.just(userRequestDTO));
        when(patientMapper.toResponseWithUserInfo(patient, userRequestDTO)).thenReturn(patientResponseDTO);

        StepVerifier.create(patientService.getPatientByMedicalRecordNumber("MED-2026-000001"))
                .expectNext(patientResponseDTO)
                .verifyComplete();
    }

    @Test
    @DisplayName("getPatientsByBloodType - retourne la liste")
    void getByBloodType_ShouldReturnList() {
        when(patientRepository.findByBloodTypeAndActiveTrue("A+")).thenReturn(List.of(patient));
        when(patientMapper.toResponse(patient)).thenReturn(patientResponseDTO);

        StepVerifier.create(patientService.getPatientsByBloodType("A+"))
                .expectNext(patientResponseDTO)
                .verifyComplete();
    }

    @Test
    @DisplayName("hasPatientRecord - true si existe")
    void hasPatientRecord_ShouldReturnTrue() {
        when(patientRepository.existsByUserUuid("user-uuid-001")).thenReturn(true);

        StepVerifier.create(patientService.hasPatientRecord("user-uuid-001"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("countActivePatients - retourne le nombre")
    void countActivePatients_ShouldReturnCount() {
        when(patientRepository.countByActiveTrue()).thenReturn(5L);

        StepVerifier.create(patientService.countActivePatients())
                .expectNext(5L)
                .verifyComplete();
    }

    @Nested
    @DisplayName("getPatientByEmail - Tests")
    class GetPatientByEmailTests {

        @Test
        @DisplayName("getPatientByEmail - succès avec email existant")
        void getPatientByEmail_ShouldReturnResponse() {
            // Given
            String email = "john@test.com";
            when(userService.getUserByEmail(email)).thenReturn(Mono.just(userRequestDTO));
            when(patientRepository.findByUserUuid("user-uuid-001")).thenReturn(Optional.of(patient));
            when(patientMapper.toResponseWithUserInfo(patient, userRequestDTO)).thenReturn(patientResponseDTO);

            // When & Then
            StepVerifier.create(patientService.getPatientByEmail(email))
                    .expectNext(patientResponseDTO)
                    .verifyComplete();

            verify(userService).getUserByEmail(email);
            verify(patientRepository).findByUserUuid("user-uuid-001");
            verify(patientMapper).toResponseWithUserInfo(patient, userRequestDTO);
        }

        @Test
        @DisplayName("getPatientByEmail - erreur si utilisateur non trouvé")
        void getPatientByEmail_ShouldFailIfUserNotFound() {
            // Given
            String email = "unknown@test.com";
            when(userService.getUserByEmail(email)).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(patientService.getPatientByEmail(email))
                    .expectErrorMatches(throwable -> throwable instanceof ApiException
                            && throwable.getMessage().equals("Aucun utilisateur trouvé avec cet email: " + email))
                    .verify();

            verify(userService).getUserByEmail(email);
            verify(patientRepository, never()).findByUserUuid(anyString());
        }

        @Test
        @DisplayName("getPatientByEmail - erreur si utilisateur existe mais pas de dossier patient")
        void getPatientByEmail_ShouldFailIfPatientNotFound() {
            // Given
            String email = "john@test.com";
            when(userService.getUserByEmail(email)).thenReturn(Mono.just(userRequestDTO));
            when(patientRepository.findByUserUuid("user-uuid-001")).thenReturn(Optional.empty());

            // When & Then
            StepVerifier.create(patientService.getPatientByEmail(email))
                    .expectErrorMatches(throwable -> throwable instanceof ApiException
                            && throwable.getMessage().equals("Aucun dossier patient pour l'email: " + email))
                    .verify();

            verify(userService).getUserByEmail(email);
            verify(patientRepository).findByUserUuid("user-uuid-001");
            verify(patientMapper, never()).toResponseWithUserInfo(any(), any());
        }

        @Test
        @DisplayName("getPatientByEmail - erreur si service utilisateur échoue")
        void getPatientByEmail_ShouldFailIfUserServiceFails() {
            // Given
            String email = "john@test.com";
            when(userService.getUserByEmail(email)).thenReturn(Mono.error(new RuntimeException("Service indisponible")));

            // When & Then
            StepVerifier.create(patientService.getPatientByEmail(email))
                    .expectError(RuntimeException.class)
                    .verify();

            verify(userService).getUserByEmail(email);
            verify(patientRepository, never()).findByUserUuid(anyString());
        }

        @Test
        @DisplayName("getPatientByEmail - vérifie l'enrichissement des données utilisateur")
        void getPatientByEmail_ShouldEnrichWithUserInfo() {
            // Given
            String email = "john@test.com";
            PatientResponseDTO enrichedResponse = PatientResponseDTO.builder()
                    .patientUuid("patient-uuid-001")
                    .medicalRecordNumber("MED-2026-000001")
                    .userInfo(PatientResponseDTO.UserInfo.builder()
                            .firstName("John")
                            .lastName("Doe")
                            .email("john@test.com")
                            .build())
                    .build();

            when(userService.getUserByEmail(email)).thenReturn(Mono.just(userRequestDTO));
            when(patientRepository.findByUserUuid("user-uuid-001")).thenReturn(Optional.of(patient));
            when(patientMapper.toResponseWithUserInfo(patient, userRequestDTO)).thenReturn(enrichedResponse);

            // When & Then
            StepVerifier.create(patientService.getPatientByEmail(email))
                    .expectNextMatches(response ->
                            response.getPatientUuid().equals("patient-uuid-001") &&
                                    response.getUserInfo() != null &&
                                    response.getUserInfo().getEmail().equals("john@test.com"))
                    .verifyComplete();

            verify(patientMapper).toResponseWithUserInfo(patient, userRequestDTO);
        }
    }

    @Nested
    @DisplayName("updatePatient - Tests supplémentaires")
    class UpdatePatientAdditionalTests {

        @Test
        @DisplayName("updatePatient - avec mise à jour des coordonnées utilisateur")
        void updatePatient_ShouldUpdateUserContactInfo() {
            // Given
            PatientRequestDTO request = new PatientRequestDTO();
            request.setPhone("+33123456789");
            request.setAddress("123 Rue de Paris");
            request.setDateOfBirth(LocalDate.of(1990, 5, 15));

            UserRequestDTO updatedUser = new UserRequestDTO();
            updatedUser.setUserUuid("user-uuid-001");
            updatedUser.setFirstName("John");
            updatedUser.setLastName("Doe");
            updatedUser.setEmail("john@test.com");
            updatedUser.setPhone("+33123456789");
            updatedUser.setAddress("123 Rue de Paris");

            Patient updatedPatient = patient.toBuilder().bloodType("B+").build();

            when(patientRepository.findByPatientUuid("patient-uuid-001")).thenReturn(Optional.of(patient));
            when(userService.getUserByUuid("user-uuid-001")).thenReturn(Mono.just(userRequestDTO));
            when(userService.updateUserContactInfo(eq("user-uuid-001"), eq("+33123456789"), eq("123 Rue de Paris")))
                    .thenReturn(Mono.just(updatedUser));
            when(patientMapper.updateEntity(patient, request)).thenReturn(updatedPatient);
            when(patientRepository.save(updatedPatient)).thenReturn(updatedPatient);
            when(patientMapper.toResponseWithUserInfo(updatedPatient, updatedUser)).thenReturn(patientResponseDTO);

            // When & Then
            StepVerifier.create(patientService.updatePatient("patient-uuid-001", request))
                    .expectNext(patientResponseDTO)
                    .verifyComplete();

            verify(userService).updateUserContactInfo("user-uuid-001", "+33123456789", "123 Rue de Paris");
            verify(patientRepository).save(updatedPatient);
        }

        @Test
        @DisplayName("updatePatient - mise à jour utilisateur échoue mais continue avec anciennes données")
        void updatePatient_ShouldContinueWhenUserUpdateFails() {
            // Given
            PatientRequestDTO request = new PatientRequestDTO();
            request.setPhone("+33123456789");
            request.setDateOfBirth(LocalDate.of(1990, 5, 15));

            Patient updatedPatient = patient.toBuilder().bloodType("B+").build();

            when(patientRepository.findByPatientUuid("patient-uuid-001")).thenReturn(Optional.of(patient));
            when(userService.getUserByUuid("user-uuid-001")).thenReturn(Mono.just(userRequestDTO));
            when(userService.updateUserContactInfo(eq("user-uuid-001"), eq("+33123456789"), isNull()))
                    .thenReturn(Mono.error(new RuntimeException("Service indisponible")));
            when(patientMapper.updateEntity(patient, request)).thenReturn(updatedPatient);
            when(patientRepository.save(updatedPatient)).thenReturn(updatedPatient);
            when(patientMapper.toResponseWithUserInfo(updatedPatient, userRequestDTO)).thenReturn(patientResponseDTO);

            // When & Then
            StepVerifier.create(patientService.updatePatient("patient-uuid-001", request))
                    .expectNext(patientResponseDTO)
                    .verifyComplete();

            verify(userService).updateUserContactInfo(eq("user-uuid-001"), eq("+33123456789"), isNull());
            verify(patientRepository).save(updatedPatient);
        }

        @Test
        @DisplayName("updatePatient - seulement l'adresse est mise à jour")
        void updatePatient_ShouldUpdateOnlyAddress() {
            // Given
            PatientRequestDTO request = new PatientRequestDTO();
            request.setAddress("456 New Street");
            request.setDateOfBirth(LocalDate.of(1990, 5, 15));

            UserRequestDTO updatedUser = new UserRequestDTO();
            updatedUser.setUserUuid("user-uuid-001");
            updatedUser.setFirstName("John");
            updatedUser.setLastName("Doe");
            updatedUser.setEmail("john@test.com");
            updatedUser.setAddress("456 New Street");

            Patient updatedPatient = patient.toBuilder().bloodType("B+").build();

            when(patientRepository.findByPatientUuid("patient-uuid-001")).thenReturn(Optional.of(patient));
            when(userService.getUserByUuid("user-uuid-001")).thenReturn(Mono.just(userRequestDTO));
            when(userService.updateUserContactInfo(eq("user-uuid-001"), isNull(), eq("456 New Street")))
                    .thenReturn(Mono.just(updatedUser));
            when(patientMapper.updateEntity(patient, request)).thenReturn(updatedPatient);
            when(patientRepository.save(updatedPatient)).thenReturn(updatedPatient);
            when(patientMapper.toResponseWithUserInfo(updatedPatient, updatedUser)).thenReturn(patientResponseDTO);

            // When & Then
            StepVerifier.create(patientService.updatePatient("patient-uuid-001", request))
                    .expectNext(patientResponseDTO)
                    .verifyComplete();

            verify(userService).updateUserContactInfo(eq("user-uuid-001"), isNull(), eq("456 New Street"));
        }
    }
}