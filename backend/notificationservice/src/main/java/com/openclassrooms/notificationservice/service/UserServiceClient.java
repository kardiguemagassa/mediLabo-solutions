package com.openclassrooms.notificationservice.service;

import com.openclassrooms.notificationservice.dtorequest.UserRequest;
import reactor.core.publisher.Mono;

public interface UserServiceClient {
    Mono<UserRequest> getUserByEmail(String email);
    Mono<UserRequest> getUserByUuid(String userUuid);
}