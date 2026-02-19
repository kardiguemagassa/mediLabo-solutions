//package com.openclassrooms.authorizationserverservice.controller;
//
////import com.openclassrooms.authorizationserverservice.dto.UserInfoDTO;
//import com.openclassrooms.authorizationserverservice.service.UserService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerAutoConfiguration;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//
//import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.UUID;
//
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = {OAuth2AuthorizationServerAutoConfiguration.class})
//class UserControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockitoBean
//    private UserService userService;
//
//    @MockitoBean
//    private RegisteredClientRepository registeredClientRepository;
//
//    @Test
//    @DisplayName("GET /api/users/{uuid} - Succès avec autorité ADMIN")
//    @WithMockUser(authorities = "ADMIN")
//    void getUserInfo_Success() throws Exception {
//        String uuid = UUID.randomUUID().toString();
//        UserInfoDTO dto = UserInfoDTO.builder()
//                .userUuid(uuid)
//                .email("test@example.com")
//                .build();
//
//        when(userService.getUserInfoByUuid(uuid)).thenReturn(dto);
//
//        mockMvc.perform(get("/api/users/" + uuid))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.userUuid").value(uuid));
//    }
//
//    @Test
//    @DisplayName("GET /api/users/{uuid}/exists - Success")
//    @WithMockUser(authorities = "PRACTITIONER")
//    void userExists_ShouldReturnTrue() throws Exception {
//        String uuid = UUID.randomUUID().toString();
//
//        // Mock du service
//        when(userService.userExistsByUuid(uuid)).thenReturn(true);
//
//        mockMvc.perform(get("/api/users/" + uuid + "/exists"))
//                .andExpect(status().isOk())
//                .andExpect(content().string("true"));
//    }
//
//    @Test
//    @DisplayName("GET /api/users/{uuid}/exists - Succès PRACTITIONER")
//    @WithMockUser(authorities = "PRACTITIONER")
//    void userExists_True() throws Exception {
//        String uuid = UUID.randomUUID().toString();
//        when(userService.userExistsByUuid(uuid)).thenReturn(true);
//
//        mockMvc.perform(get("/api/users/" + uuid + "/exists"))
//                .andExpect(status().isOk())
//                .andExpect(content().string("true"));
//    }
//}