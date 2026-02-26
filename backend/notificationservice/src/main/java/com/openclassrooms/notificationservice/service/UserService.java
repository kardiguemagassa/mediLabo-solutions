package com.openclassrooms.notificationservice.service;

import com.openclassrooms.notificationservice.dtorequest.UserRequest;
import com.openclassrooms.notificationservice.dtoresponse.MessageResponse;
import reactor.core.publisher.Mono;

public interface UserService {
    Mono<UserRequest> getUserByEmail(String email);
    Mono<UserRequest> getUserByUuid(String userUuid);
}