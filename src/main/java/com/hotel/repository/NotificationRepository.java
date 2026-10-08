package com.hotel.repository;

import com.hotel.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for internal notifications.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUsernameOrderByCreatedAtDesc(String username);

    long countByRecipientUsernameAndIsReadFalse(String username);

    Optional<Notification> findByIdAndRecipientUsername(Long id, String username);

    Optional<Notification> findByMessageIdAndRecipientUsername(Long messageId, String username);
}
