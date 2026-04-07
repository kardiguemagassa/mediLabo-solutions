package com.openclassrooms.notificationservice.repository;

import com.openclassrooms.notificationservice.model.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface MessageStatusRepository extends JpaRepository<MessageStatus, Long> {

    @Query("SELECT COUNT(ms) FROM MessageStatus ms WHERE ms.userUuid = :userUuid AND ms.messageStatus = 'UNREAD'")
    int countUnread(@Param("userUuid") String userUuid);

    @Transactional
    @Modifying
    @Query("UPDATE MessageStatus ms SET ms.messageStatus = :status, ms.readAt = CURRENT_TIMESTAMP " + "WHERE ms.userUuid = :userUuid AND ms.message.messageId = :messageId")
    int updateStatus(@Param("userUuid") String userUuid, @Param("messageId") Long messageId, @Param("status") String status);
}