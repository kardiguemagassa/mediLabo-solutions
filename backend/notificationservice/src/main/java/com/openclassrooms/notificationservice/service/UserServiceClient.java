package com.openclassrooms.notificationservice.service;

import com.openclassrooms.notificationservice.dto.UserRequestDTO;
import reactor.core.publisher.Mono;

public interface UserServiceClient {
    Mono<UserRequestDTO> getUserByEmail(String email);
    Mono<UserRequestDTO> getUserByUuid(String userUuid);
}