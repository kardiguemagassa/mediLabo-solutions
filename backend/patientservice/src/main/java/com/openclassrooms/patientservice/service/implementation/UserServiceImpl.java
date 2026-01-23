package com.openclassrooms.patientservice.service.implementation;

import com.openclassrooms.patientservice.domain.Response;
import com.openclassrooms.patientservice.dtorequest.UserRequest;
import com.openclassrooms.patientservice.exception.ApiException;
import com.openclassrooms.patientservice.handler.RestClientInterceptor;
import com.openclassrooms.patientservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


import static com.openclassrooms.patientservice.util.RequestUtils.convertResponse;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    /*private final RestClient restClient;

    public UserServiceImpl() {
        this.restClient = RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory())
                .baseUrl("http://localhost:9001")
                .requestInterceptor(new RestClientInterceptor())
                .build();
    }


    @Override
    public UserRequest getUserByUuid(String userUuid) {
        try {
            var response = restClient.get().uri("/user/profile").retrieve().body(Response.class);
            assert response != null;
            return convertResponse(response, UserRequest.class,"user");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }

    }

    @Override
    public UserRequest getAssignee(String patientUuid) {
        try {
            var response = restClient.get().uri("/user/assignee" + patientUuid).retrieve().body(Response.class);
            assert response != null;
            return convertResponse(response, UserRequest.class,"user");
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Une erreur s'est produite. Veuillez réessayer.");
        }
    }*/


    private final WebClient webClient;

    public UserServiceImpl(WebClient userWebClient) {
        this.webClient = userWebClient;
    }


    @Override
    public UserRequest getUserByUuid(String userUuid) {
        Response response = webClient.get().uri("/user/profile").retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> Mono.error(new ApiException("Erreur lors de l'appel user-service")))
                .bodyToMono(Response.class).switchIfEmpty(Mono.error(new ApiException("Réponse vide du service utilisateur"))).block();

        return convertResponse(response, UserRequest.class,"user");

    }


    @Override
    public UserRequest getAssignee(String patientUuid) {

        Response response = webClient.get().uri("/user/assignee/{uuid}", patientUuid).retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> Mono.error(new ApiException("Erreur lors de l'appel user-service")))
                .bodyToMono(Response.class).switchIfEmpty(Mono.error(new ApiException("Réponse vide du service utilisateur"))).block();

        return convertResponse(response, UserRequest.class, "user");
    }

    @Override
    public UserRequest getUserByUuid(String userUuid, String bearerToken) {
        return null;
    }

    @Override
    public UserRequest getAssignee(String patientUuid, String bearerToken) {
        return null;
    }


}
