package com.openclassrooms.assessmentservice.service.implementation;

import com.openclassrooms.assessmentservice.model.Gender;
import com.openclassrooms.assessmentservice.model.RiskLevel;
import com.openclassrooms.assessmentservice.exception.ApiException;
import com.openclassrooms.assessmentservice.dtoresponse.NoteResponseDTO;
import com.openclassrooms.assessmentservice.dtoresponse.PatientResponseDTO;
import com.openclassrooms.assessmentservice.service.RiskLevelCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssessmentServiceImpl - Tests unitaires")
@MockitoSettings(strictness = Strictness.LENIENT)
class AssessmentServiceImplTest {

    @Mock
    private PatientServiceClientImpl patientServiceClient;

    @Mock
    private NoteServiceClientImpl noteServiceClient;

    @Mock
    private RiskLevelCalculator riskLevelCalculator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AssessmentServiceImpl assessmentService;

    private static final String PATIENT_UUID = "patient-uuid-123";
    private static final String TEST_TOKEN = "test-jwt-token";

    @BeforeEach
    void setUp() {}

    private PatientResponseDTO createPatient(int age, Gender gender) {
        return PatientResponseDTO.builder()
                .patientUuid(PATIENT_UUID)
                .dateOfBirth(LocalDate.now().minusYears(age))
                .gender(gender)
                .userInfo(new PatientResponseDTO.UserInfo(
                        "Jean",
                        "Dupont",
                        "jean.dupont@email.com",
                        null
                ))
                .build();
    }

    private NoteResponseDTO createNote(String content) {
        return NoteResponseDTO.builder()
                .noteUuid("note-uuid-" + System.nanoTime())
                .patientUuid(PATIENT_UUID)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // SUCCESS

    @Nested
    @DisplayName("Cas de succès")
    class SuccessTests {

        @Test
        @DisplayName("Devrait évaluer BORDERLINE avec 2 déclencheurs pour patient > 30 ans")
        void shouldReturnBorderline_whenOver30With2Triggers() {
            PatientResponseDTO patient = createPatient(45, Gender.FEMALE);

            List<NoteResponseDTO> notes = List.of(createNote("Patient fumeur, cholestérol élevé"), createNote("Poids anormal"));

            when(patientServiceClient.getPatientByUuid(eq(PATIENT_UUID), anyString())).thenReturn(Mono.just(patient));
            when(noteServiceClient.getNotesByPatientUuid(eq(PATIENT_UUID), anyString())).thenReturn(Flux.fromIterable(notes));
            when(riskLevelCalculator.calculate(45, Gender.FEMALE, 4)).thenReturn(RiskLevel.BORDERLINE);

            doNothing().when(eventPublisher).publishEvent(any());

            StepVerifier.create(assessmentService.assessDiabetesRisk(PATIENT_UUID, TEST_TOKEN))
                    .assertNext(result -> {
                        assertThat(result.riskLevel()).isEqualTo(RiskLevel.BORDERLINE);
                        assertThat(result.triggerCount()).isEqualTo(4);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Devrait évaluer IN_DANGER pour homme < 30 ans avec 3 déclencheurs")
        void shouldReturnInDanger_whenYoungMaleWith3Triggers() {
            PatientResponseDTO patient = createPatient(25, Gender.MALE);

            List<NoteResponseDTO> notes = List.of(createNote("Fumeur, taille anormale, poids élevé"));

            //when(patientServiceClient.getPatientByUuid(PATIENT_UUID)).thenReturn(Mono.just(patient));
            when(patientServiceClient.getPatientByUuid(eq(PATIENT_UUID), anyString())).thenReturn(Mono.just(patient));
            when(noteServiceClient.getNotesByPatientUuid(eq(PATIENT_UUID), anyString())).thenReturn(Flux.fromIterable(notes));
            when(riskLevelCalculator.calculate(25, Gender.MALE, 4)).thenReturn(RiskLevel.IN_DANGER);

            doNothing().when(eventPublisher).publishEvent(any());

            StepVerifier.create(assessmentService.assessDiabetesRisk(PATIENT_UUID, TEST_TOKEN))
                    .assertNext(result -> {
                        assertThat(result.riskLevel()).isEqualTo(RiskLevel.IN_DANGER);
                        assertThat(result.triggerCount()).isEqualTo(4);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Devrait évaluer EARLY_ONSET pour homme < 30 ans avec 5+ déclencheurs")
        void shouldReturnEarlyOnset_whenYoungMaleWith5Triggers() {
            PatientResponseDTO patient = createPatient(28, Gender.MALE);

            List<NoteResponseDTO> notes = List.of(
                    createNote("Fumeur, cholestérol anormal, vertiges"),
                    createNote("Anticorps détectés, réaction allergique")
            );

            when(patientServiceClient.getPatientByUuid(eq(PATIENT_UUID), anyString())).thenReturn(Mono.just(patient));
            when(noteServiceClient.getNotesByPatientUuid(eq(PATIENT_UUID), anyString())).thenReturn(Flux.fromIterable(notes));
            when(riskLevelCalculator.calculate(28, Gender.MALE, 6)).thenReturn(RiskLevel.EARLY_ONSET);

            doNothing().when(eventPublisher).publishEvent(any());

            StepVerifier.create(assessmentService.assessDiabetesRisk(PATIENT_UUID,TEST_TOKEN))
                    .assertNext(result -> {
                        assertThat(result.riskLevel()).isEqualTo(RiskLevel.EARLY_ONSET);
                        assertThat(result.triggerCount()).isEqualTo(6);
                    })
                    .verifyComplete();
        }
    }

    // ERROR

    @Nested
    @DisplayName("Cas d'erreur")
    class ErrorTests {

        @Test
        @DisplayName("Devrait lever ApiException si patient introuvable")
        void shouldThrowPatientNotFound_whenPatientDoesNotExist() {
            when(patientServiceClient.getPatientByUuid(eq(PATIENT_UUID), anyString())).thenReturn(Mono.empty());

            // IMPORTANT: Il faut aussi mocker le service de notes pour éviter NullPointerException
            when(noteServiceClient.getNotesByPatientUuid(eq(PATIENT_UUID), anyString())).thenReturn(Flux.empty());

            StepVerifier.create(assessmentService.assessDiabetesRisk(PATIENT_UUID, TEST_TOKEN))
                    .expectErrorMatches(error ->
                            error instanceof ApiException &&
                                    error.getMessage().contains(PATIENT_UUID))
                    .verify();

            verify(patientServiceClient).getPatientByUuid(PATIENT_UUID, TEST_TOKEN);
            verify(noteServiceClient).getNotesByPatientUuid(PATIENT_UUID, TEST_TOKEN);
            verify(riskLevelCalculator, never()).calculate(anyInt(), any(), anyInt());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Devrait propager l'erreur si Notes Service indisponible")
        void shouldPropagateError_whenNotesServiceUnavailable() {
            PatientResponseDTO patient = createPatient(50, Gender.FEMALE);
            RuntimeException notesError = new RuntimeException("Notes down");

            when(patientServiceClient.getPatientByUuid(eq(PATIENT_UUID), anyString())).thenReturn(Mono.just(patient));
            when(noteServiceClient.getNotesByPatientUuid(eq(PATIENT_UUID), anyString())).thenReturn(Flux.error(notesError));

            StepVerifier.create(assessmentService.assessDiabetesRisk(PATIENT_UUID, TEST_TOKEN)).expectError(RuntimeException.class).verify();

            verify(patientServiceClient).getPatientByUuid(eq(PATIENT_UUID), anyString());
            verify(noteServiceClient).getNotesByPatientUuid(eq(PATIENT_UUID), anyString());
            verify(riskLevelCalculator, never()).calculate(anyInt(), any(), anyInt());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    // CAS LIMITES

    @Nested
    @DisplayName("Cas limites")
    class EdgeCasesTests {

        @Test
        @DisplayName("Devrait gérer les notes avec contenu null")
        void shouldHandleNotesWithNullContent() {
            PatientResponseDTO patient = createPatient(35, Gender.MALE);

            NoteResponseDTO note = createNote(null);

            when(patientServiceClient.getPatientByUuid(eq(PATIENT_UUID), anyString())).thenReturn(Mono.just(patient));
            when(noteServiceClient.getNotesByPatientUuid(eq(PATIENT_UUID), anyString())).thenReturn(Flux.just(note));
            when(riskLevelCalculator.calculate(35, Gender.MALE, 0)).thenReturn(RiskLevel.NONE);

            doNothing().when(eventPublisher).publishEvent(any());

            StepVerifier.create(assessmentService.assessDiabetesRisk(PATIENT_UUID, TEST_TOKEN))
                    .assertNext(result -> {
                        assertThat(result.triggerCount()).isZero();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Devrait gérer les notes avec contenu vide")
        void shouldHandleNotesWithEmptyContent() {
            PatientResponseDTO patient = createPatient(35, Gender.FEMALE);

            NoteResponseDTO note = createNote("");

            when(patientServiceClient.getPatientByUuid(eq(PATIENT_UUID), anyString())).thenReturn(Mono.just(patient));
            when(noteServiceClient.getNotesByPatientUuid(eq(PATIENT_UUID), anyString())).thenReturn(Flux.just(note));
            when(riskLevelCalculator.calculate(35, Gender.FEMALE, 0)).thenReturn(RiskLevel.NONE);

            doNothing().when(eventPublisher).publishEvent(any());

            StepVerifier.create(assessmentService.assessDiabetesRisk(PATIENT_UUID, TEST_TOKEN))
                    .assertNext(result -> {
                        assertThat(result.triggerCount()).isZero();
                    })
                    .verifyComplete();
        }
    }
}