package com.openclassrooms.assessmentservice.controller;

import com.openclassrooms.assessmentservice.dtoresponse.AssessmentResponse;
import com.openclassrooms.assessmentservice.mapper.AssessmentMapper;
import com.openclassrooms.assessmentservice.model.Assessment;
import com.openclassrooms.assessmentservice.model.Gender;
import com.openclassrooms.assessmentservice.model.RiskLevel;
import com.openclassrooms.assessmentservice.service.AssessmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AssessmentController - Tests d'intégration")
class AssessmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssessmentService assessmentService;

    @MockitoBean
    private AssessmentMapper assessmentMapper;

    private static final String PATIENT_UUID = "patient-uuid-123";
    private static final String BASE_URL = "/api/assessments/diabetes";

    @Test
    @WithMockUser
    @DisplayName("Scénario Succès : Calcul du risque asynchrone")
    void scenario_AssessmentSuccess() throws Exception {

        Assessment assessment = new Assessment(PATIENT_UUID, "Jean Dupont", 45, Gender.MALE, RiskLevel.NONE, 0, List.of(), LocalDateTime.now());

        AssessmentResponse assessmentResponse = AssessmentResponse.builder()
                .patientUuid(PATIENT_UUID)
                .patientName("Jean Dupont")
                .age(45)
                .gender(Gender.MALE)
                .riskLevel(RiskLevel.NONE)
                .riskLevelDescription("Aucun risque")
                .triggerCount(0)
                .triggersFound(List.of())
                .assessedAt(LocalDateTime.now())
                .build();

        Mockito.when(assessmentService.assessDiabetesRisk(eq(PATIENT_UUID), any())).thenReturn(Mono.just(assessment));
        Mockito.when(assessmentMapper.toResponse(any(Assessment.class))).thenReturn(assessmentResponse);

        MvcResult mvcResult = mockMvc.perform(
                        get(BASE_URL + "/{patientUuid}", PATIENT_UUID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assessment.patientUuid", is(PATIENT_UUID)))
                .andExpect(jsonPath("$.data.assessment.patientName", is("Jean Dupont")))
                .andExpect(jsonPath("$.data.assessment.age", is(45)))
                .andExpect(jsonPath("$.data.assessment.riskLevel", is("NONE")))
                .andExpect(jsonPath("$.data.assessment.triggerCount", is(0)));
    }

    @Test
    @WithMockUser
    @DisplayName("Scénario Erreur : Gestion d'exception asynchrone")
    void scenario_AssessmentFailure() throws Exception {

        Mockito.when(assessmentService.assessDiabetesRisk(eq(PATIENT_UUID), any())).thenReturn(Mono.error(new RuntimeException("Database error")));

        MvcResult mvcResult = mockMvc.perform(
                        get(BASE_URL + "/{patientUuid}", PATIENT_UUID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser
    @DisplayName("Scénario Succès : Patient à risque IN_DANGER")
    void scenario_AssessmentInDanger() throws Exception {

        List<String> triggers = List.of("Poids", "Fumeur", "Cholestérol", "Vertiges", "Taille");

        Assessment assessment = new Assessment(PATIENT_UUID, "Marie Martin", 25, Gender.FEMALE, RiskLevel.IN_DANGER, 5, triggers, LocalDateTime.now());

        AssessmentResponse assessmentResponse = AssessmentResponse.builder()
                .patientUuid(PATIENT_UUID)
                .patientName("Marie Martin")
                .age(25)
                .gender(Gender.FEMALE)
                .riskLevel(RiskLevel.IN_DANGER)
                .riskLevelDescription("Danger")
                .triggerCount(5)
                .triggersFound(triggers)
                .assessedAt(LocalDateTime.now())
                .build();

        Mockito.when(assessmentService.assessDiabetesRisk(eq(PATIENT_UUID), any())).thenReturn(Mono.just(assessment));
        Mockito.when(assessmentMapper.toResponse(any(Assessment.class))).thenReturn(assessmentResponse);

        MvcResult mvcResult = mockMvc.perform(
                        get(BASE_URL + "/{patientUuid}", PATIENT_UUID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assessment.riskLevel", is("IN_DANGER")))
                .andExpect(jsonPath("$.data.assessment.triggerCount", is(5)))
                .andExpect(jsonPath("$.data.assessment.triggersFound", is(triggers)));
    }
}