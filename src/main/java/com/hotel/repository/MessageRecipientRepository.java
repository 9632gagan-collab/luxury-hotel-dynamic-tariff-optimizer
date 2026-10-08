package com.hotel.repository;

import com.hotel.model.MessageRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for internal message recipient states.
 */
@Repository
public interface MessageRecipientRepository extends JpaRepository<MessageRecipient, Long> {

    Optional<MessageRecipient> findByMessageIdAndRecipientUsername(Long messageId, String username);

    long countByRecipientUsernameAndIsReadFalse(String username);

    List<MessageRecipient> findByRecipientUsernameAndIsReadFalse(String username);

    List<MessageRecipient> findByMessageId(Long messageId);
}
