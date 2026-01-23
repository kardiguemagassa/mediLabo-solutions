package com.openclassrooms.patientservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.patientservice.domain.Response;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;


import java.util.Map;

import static java.time.LocalTime.now;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class RequestUtils {


    /**
     * Crée une réponse standardisée pour une requête réussie.
     *
     * @param request requête HTTP
     * @param data données à inclure dans la réponse
     * @param message message à afficher
     * @param status code HTTP
     * @return objet {@link Response} construit
     **/

    public static Response getResponse(HttpServletRequest request, Map<?, ?> data, String message, HttpStatus status) {
        return new Response(now().toString(), status.value(), request.getRequestURI(), status, message, EMPTY, data);
    }

    public static <T> T convertResponse(Response response, Class<T> type, String keyName) {
        var mapper = new ObjectMapper();
        return mapper.convertValue(response.data().get(keyName),type);
    }


}
