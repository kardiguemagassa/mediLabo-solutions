package com.openclassrooms.patientservice.service;

import com.openclassrooms.patientservice.dtorequest.UserRequest;


public interface UserService {

    UserRequest getUserByUuid(String userUuid);
    UserRequest getAssignee(String patientUuid);

    UserRequest getUserByUuid(String userUuid, String bearerToken);

    UserRequest getAssignee(String patientUuid, String bearerToken);
}
