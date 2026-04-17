package com.openclassrooms.patientservice.service.implementation;

import com.openclassrooms.patientservice.dto.PatientRequestDTO;
import com.openclassrooms.patientservice.dto.UserRequestDTO;
import com.openclassrooms.patientservice.dto.PatientResponseDTO;
import com.openclassrooms.patientservice.event.Event;
import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.mapper.PatientMapper;
import com.openclassrooms.patientservice.model.Patient;
import com.openclassrooms.patientservice.repository.PatientRepository;
import com.openclassrooms.patientservice.service.PatientService;
import com.openclassrooms.patientservice.service.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;

import static com.openclassrooms.patientservice.enumeration.EventType.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final UserServiceClient userService;
    private final ApplicationEventPublisher eventPublisher;

    // CRUD OPERATIONS

    @Override
    public Mono<Page<PatientResponseDTO>> getAllPatientsPageable(Pageable pageable) {
        return Mono.fromCallable(() -> patientRepository.findAllByOrderByCreatedAtDesc(pageable))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(page -> {
                    List<Patient> patients = page.getContent();
                    if (patients.isEmpty()) {
                        return Mono.just(page.map(patientMapper::toResponse));
                    }
                    // Enrichir chaque patient avec les infos utilisateur
                    return Flux.fromIterable(patients)
                            .flatMap(patient -> enrichWithUserInfo(patient)
                                    .onErrorResume(e -> {
                                        log.warn("Could not enrich patient {}: {}", patient.getPatientUuid(), e.getMessage());
                                        return Mono.just(patientMapper.toResponse(patient));
                                    }))
                            .collectList()
                            .map(enrichedList -> new org.springframework.data.domain.PageImpl<>(
                                    enrichedList, page.getPageable(), page.getTotalElements()));
                });
    }

    @Override
    @Transactional
    public Mono<PatientResponseDTO> createPatient(PatientRequestDTO request) {
        log.info("Creating patient for user: {}", request.getUserUuid());

        return Mono.fromCallable(() -> patientRepository.existsByUserUuid(request.getUserUuid()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ApiException("Un dossier patient existe déjà pour cet utilisateur"));
                    }
                    return userService.getUserByUuid(request.getUserUuid());
                })
                .flatMap(user -> {
                    String medicalRecordNumber = generateUniqueMedicalRecordNumber();

                    return Mono.fromCallable(() -> {
                                Patient patient = patientMapper.toEntity(request, medicalRecordNumber);
                                Patient savedPatient = patientRepository.save(patient);

                                eventPublisher.publishEvent(Event.builder()
                                        .eventType(PATIENT_CREATED)
                                        .data(Map.of(
                                                "email", user.getEmail(),
                                                "name", user.getFirstName() + " " + user.getLastName(),
                                                "recordNumber", medicalRecordNumber,
                                                "subject", "Bienvenue chez MediLabo"
                                        ))
                                        .build());

                                log.info("Patient created successfully: {}", savedPatient.getPatientUuid());
                                return patientMapper.toResponse(savedPatient);
                            })
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }

    @Override
    public Mono<PatientResponseDTO> getPatientByUuid(String patientUuid) {
        log.debug("Fetching patient by UUID: {}", patientUuid);

        return Mono.fromCallable(() -> patientRepository.findByPatientUuid(patientUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new ApiException("Patient non trouvé: " + patientUuid))))
                .flatMap(this::enrichWithUserInfo);
    }

    @Override
    public Mono<PatientResponseDTO> getPatientByUserUuid(String userUuid) {
        log.debug("Fetching patient for user: {}", userUuid);

        return Mono.fromCallable(() -> patientRepository.findByUserUuid(userUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new ApiException("Aucun dossier patient pour cet utilisateur"))))
                .flatMap(this::enrichWithUserInfo);
    }

    @Override
    public Mono<PatientResponseDTO> getPatientByEmail(String email) {
        log.debug("Fetching patient by email: {}", email);

        return userService.getUserByEmail(email)
                .switchIfEmpty(Mono.error(new ApiException("Aucun utilisateur trouvé avec cet email: " + email)))
                .flatMap(user -> Mono.fromCallable(() -> patientRepository.findByUserUuid(user.getUserUuid()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(optional -> optional
                                .map(patient -> Mono.just(patientMapper.toResponseWithUserInfo(patient, user)))
                                .orElseGet(() -> Mono.error(new ApiException("Aucun dossier patient pour l'email: " + email)))));
    }

    @Override
    public Flux<PatientResponseDTO> getAllActivePatients() {
        return Mono.fromCallable(patientRepository::findAllByOrderByCreatedAtDesc)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .flatMap(patient -> enrichWithUserInfo(patient)
                        .onErrorResume(e -> {
                            log.warn("Could not enrich patient {}: {}", patient.getPatientUuid(), e.getMessage());
                            return Mono.just(patientMapper.toResponse(patient));
                        }));
    }

    @Override
    public Mono<Page<PatientResponseDTO>> getAllActivePatientsPageable(Pageable pageable) {
        return Mono.fromCallable(() -> patientRepository.findAllByOrderByCreatedAtDesc(pageable))
                .subscribeOn(Schedulers.boundedElastic())
                .map(page -> page.map(patientMapper::toResponse));
    }

    @Override
    @Transactional
    public Mono<PatientResponseDTO> updatePatient(String patientUuid, PatientRequestDTO request) {
        log.info("Updating patient: {}", patientUuid);

        return Mono.fromCallable(() -> patientRepository.findByPatientUuid(patientUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new ApiException("Patient non trouvé: " + patientUuid))))
                .flatMap(existingPatient ->
                        userService.getUserByUuid(existingPatient.getUserUuid())
                                .flatMap(user -> {

                                    Mono<Patient> updatePatientMono = Mono.fromCallable(() -> {
                                        Patient updatedPatient = patientMapper.updateEntity(existingPatient, request);
                                        return patientRepository.save(updatedPatient);
                                    }).subscribeOn(Schedulers.boundedElastic());

                                    Mono<UserRequestDTO> updateUserMono;
                                    if (request.getPhone() != null || request.getAddress() != null) {
                                        updateUserMono = userService.updateUserContactInfo(
                                                existingPatient.getUserUuid(),
                                                request.getPhone(),
                                                request.getAddress()
                                        ).onErrorResume(e -> {
                                            log.warn("Could not update user contact info: {}", e.getMessage());
                                            return Mono.just(user);
                                        });
                                    } else {
                                        updateUserMono = Mono.just(user);
                                    }

                                    return Mono.zip(updatePatientMono, updateUserMono)
                                            .map(tuple -> {
                                                Patient savedPatient = tuple.getT1();
                                                UserRequestDTO updatedUser = tuple.getT2();

                                                publishPatientUpdatedEvent(updatedUser.getEmail(),
                                                        updatedUser.getFirstName() + " " + updatedUser.getLastName(),
                                                        savedPatient.getMedicalRecordNumber());

                                                log.info("Patient updated successfully: {}", patientUuid);
                                                return patientMapper.toResponseWithUserInfo(savedPatient, updatedUser);
                                            });
                                })
                );
    }

    @Override
    @Transactional
    public Mono<Void> deletePatient(String patientUuid) {
        log.info("Soft deleting patient: {}", patientUuid);

        return Mono.fromCallable(() -> patientRepository.findByPatientUuid(patientUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> {
                    if (optional.isEmpty()) {
                        return Mono.error(new ApiException("Patient non trouvé: " + patientUuid));
                    }
                    Patient patient = optional.get();
                    // Vérifie que le patient est actif avant de supprimer
                    if (!patient.getActive()) {
                        log.warn("Patient already inactive: {}", patientUuid);
                        return Mono.error(new ApiException("Ce patient est déjà désactivé"));
                    }

                    return userService.getUserByUuid(patient.getUserUuid())
                            .flatMap(user -> Mono.fromCallable(() -> {
                                        int deleted = patientRepository.softDeleteByPatientUuid(patientUuid);
                                        if (deleted > 0) {
                                            publishPatientDeletedEvent(user.getEmail(),
                                                    user.getFirstName() + " " + user.getLastName(),
                                                    patient.getMedicalRecordNumber());
                                        }
                                        return deleted > 0;
                                    })
                                    .subscribeOn(Schedulers.boundedElastic()));
                })
                .flatMap(deleted -> {
                    if (!deleted) {
                        return Mono.error(new ApiException("Erreur lors de la suppression du patient"));
                    }
                    log.info("Patient deleted successfully: {}", patientUuid);
                    return Mono.empty();
                });
    }

    @Override
    @Transactional
    public Mono<PatientResponseDTO> restorePatient(String patientUuid) {
        log.info("Restoring patient: {}", patientUuid);

        return Mono.fromCallable(() -> patientRepository.findByPatientUuidAndActiveFalse(patientUuid))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new ApiException("Patient supprimé non trouvé: " + patientUuid))))
                .flatMap(patient -> Mono.fromCallable(() -> patientRepository.restoreByPatientUuid(patientUuid))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(restored -> {
                            if (restored == 0) {
                                return Mono.error(new ApiException("Erreur lors de la restauration du patient"));
                            }
                            log.info("Patient restored successfully: {}", patientUuid);
                            return enrichWithUserInfo(patient);
                        }));
    }

    // QUERY OPERATIONS

    @Override
    public Mono<PatientResponseDTO> getPatientByMedicalRecordNumber(String medicalRecordNumber) {
        log.debug("Fetching patient by medical record: {}", medicalRecordNumber);

        return Mono.fromCallable(() -> patientRepository.findByMedicalRecordNumber(medicalRecordNumber))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new ApiException("Dossier médical non trouvé: " + medicalRecordNumber))))
                .flatMap(this::enrichWithUserInfo);
    }

    @Override
    public Flux<PatientResponseDTO> getPatientsByBloodType(String bloodType) {
        log.debug("Fetching patients by blood type: {}", bloodType);

        return Mono.fromCallable(() -> patientRepository.findByBloodTypeAndActiveTrue(bloodType))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .map(patientMapper::toResponse);
    }

    // UTILITY OPERATIONS

    @Override
    public Mono<Boolean> hasPatientRecord(String userUuid) {
        return Mono.fromCallable(() -> patientRepository.existsByUserUuid(userUuid))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Long> countActivePatients() {
        return Mono.fromCallable(patientRepository::countByActiveTrue)
                .subscribeOn(Schedulers.boundedElastic());
    }

    // PRIVATE METHODS

    private Mono<PatientResponseDTO> enrichWithUserInfo(Patient patient) {
        return userService.getUserByUuid(patient.getUserUuid())
                .map(user -> patientMapper.toResponseWithUserInfo(patient, user))
                .onErrorResume(e -> {
                    log.warn("Could not fetch user info for patient {}: {}", patient.getPatientUuid(), e.getMessage());
                    return Mono.just(patientMapper.toResponse(patient));
                });
    }

    private String generateUniqueMedicalRecordNumber() {
        String medicalRecordNumber;
        int attempts = 0;
        final int maxAttempts = 10;

        do {
            medicalRecordNumber = generateMedicalRecordNumber();
            attempts++;
            if (attempts >= maxAttempts) {
                throw new ApiException("Impossible de générer un numéro de dossier unique");
            }
        } while (patientRepository.existsByMedicalRecordNumber(medicalRecordNumber));

        return medicalRecordNumber;
    }

    private String generateMedicalRecordNumber() {
        int year = Year.now().getValue();
        int random = (int) (Math.random() * 999999);
        return String.format("MED-%d-%06d", year, random);
    }

    // EVENT PUBLISHING

    private void publishPatientUpdatedEvent(String email, String name, String recordNumber) {
        try {
            eventPublisher.publishEvent(Event.builder()
                    .eventType(PATIENT_UPDATED)
                    .data(Map.of(
                            "email", email,
                            "name", name,
                            "recordNumber", recordNumber,
                            "date", LocalDateTime.now().toString()
                    ))
                    .build());
            log.debug("PATIENT_UPDATED event published for: {}", email);
        } catch (Exception e) {
            log.error("Failed to publish PATIENT_UPDATED event: {}", e.getMessage());
        }
    }

    private void publishPatientDeletedEvent(String email, String name, String recordNumber) {
        try {
            eventPublisher.publishEvent(Event.builder()
                    .eventType(PATIENT_DELETED)
                    .data(Map.of(
                            "email", email,
                            "name", name,
                            "recordNumber", recordNumber,
                            "date", LocalDateTime.now().toString()
                    ))
                    .build());
            log.debug("PATIENT_DELETED event published for: {}", email);
        } catch (Exception e) {
            log.error("Failed to publish PATIENT_DELETED event: {}", e.getMessage());
        }
    }
}