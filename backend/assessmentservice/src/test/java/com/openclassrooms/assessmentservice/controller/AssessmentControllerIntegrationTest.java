package com.openclassrooms.assessmentservice.controller;

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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc // Permet d'utiliser MockMvc
@DisplayName("AssessmentController - Scénario d'intégration")
class AssessmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssessmentService assessmentService;

    private static final String PATIENT_UUID = "patient-uuid-123";

    @Test
    @WithMockUser
    @DisplayName("Scénario Succès : Calcul du risque asynchrone")
    void scenario_AssessmentSuccess() throws Exception {
        // 1. GIVEN : Il faut configurer le mock AVANT l'appel
        Assessment assessment = new Assessment(PATIENT_UUID, "Jean Dupont", 45, Gender.MALE, RiskLevel.NONE, 0, List.of());

        Mockito.when(assessmentService.assessDiabetesRisk(PATIENT_UUID)).thenReturn(CompletableFuture.completedFuture(assessment));

        // 2. WHEN : Déclenchement de l'appel
        MvcResult mvcResult = mockMvc.perform(get("/api/assess/diabetes/{patientUuid}", PATIENT_UUID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        // 3. THEN : Attente et vérification du résultat final
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientUuid", is(PATIENT_UUID)))
                // Note : Vérifie si ton mapper transforme bien en "M"
                .andExpect(jsonPath("$.gender", is("M")));
    }

    @Test
    @WithMockUser
    @DisplayName("Scénario Erreur : Gestion d'exception asynchrone")
    void scenario_AssessmentFailure() throws Exception {
        // GIVEN
        CompletableFuture<Assessment> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Database error"));

        Mockito.when(assessmentService.assessDiabetesRisk(PATIENT_UUID)).thenReturn(failedFuture);

        // WHEN
        MvcResult mvcResult = mockMvc.perform(get("/api/assess/diabetes/{patientUuid}", PATIENT_UUID))
                .andExpect(request().asyncStarted())
                .andReturn();

        // THEN
        mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isInternalServerError());
    }
}