package com.openclassrooms.notificationservice.repository;

import com.openclassrooms.notificationservice.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT DISTINCT m FROM Message m LEFT JOIN FETCH m.statuses " + "WHERE m.senderUuid = :userUuid OR m.receiverUuid = :userUuid " + "ORDER BY m.createdAt DESC")
    List<Message> findAllByUser(@Param("userUuid") String userUuid);

    @Query("SELECT DISTINCT m FROM Message m LEFT JOIN FETCH m.statuses " + "WHERE m.conversationId = :conversationId " + "ORDER BY m.createdAt ASC")
    List<Message> findByConversationId(@Param("conversationId") String conversationId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Message m " + "WHERE (m.senderUuid = :userUuid AND m.receiverEmail = :receiverEmail) " + "OR (m.senderEmail = :receiverEmail AND m.receiverUuid = :userUuid)")
    boolean conversationExists(@Param("userUuid") String userUuid, @Param("receiverEmail") String receiverEmail);

    @Query("SELECT m.conversationId FROM Message m " + "WHERE (m.senderUuid = :userUuid AND m.receiverEmail = :receiverEmail) " + "OR (m.senderEmail = :receiverEmail AND m.receiverUuid = :userUuid) " + "ORDER BY m.createdAt DESC")
    List<String> findConversationIds(@Param("userUuid") String userUuid, @Param("receiverEmail") String receiverEmail);
}