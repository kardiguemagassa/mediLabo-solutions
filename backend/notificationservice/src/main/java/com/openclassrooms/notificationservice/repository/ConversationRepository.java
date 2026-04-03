package com.openclassrooms.notificationservice.repository;

import com.openclassrooms.notificationservice.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByConversationUuid(String conversationUuid);
}