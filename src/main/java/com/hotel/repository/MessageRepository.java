package com.hotel.repository;

import com.hotel.model.Message;
import com.hotel.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for internal messages between Admin and Staff.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findBySenderUsernameOrderBySentAtDesc(String username);

    List<Message> findBySenderUsernameAndSenderRoleOrderBySentAtDesc(String username, UserRole senderRole);

    @Query("SELECT DISTINCT m FROM Message m JOIN m.recipients r WHERE r.recipient.username = :username ORDER BY m.sentAt DESC")
    List<Message> findReceivedMessagesByUsername(@Param("username") String username);

    @Query("SELECT DISTINCT m FROM Message m JOIN m.recipients r WHERE r.recipient.username = :username AND m.senderRole = com.hotel.model.enums.UserRole.STAFF ORDER BY m.sentAt DESC")
    List<Message> findReceivedStaffRequestsByAdminUsername(@Param("username") String username);
}
