package com.openclassrooms.notificationservice.dtoresponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {
    private String messageUuid;
    private String conversationId;
    private String subject;
    private String message;
    private String status;
    private String createdAt;
    private String updatedAt;

    private UserInfo sender;
    private UserInfo receiver;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserInfo {
        private String userUuid;
        private String name;
        private String email;
        private String imageUrl;
        private String role;
    }
}
