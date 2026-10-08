package com.hotel.service.impl;

import com.hotel.model.*;
import com.hotel.model.enums.AccountStatus;
import com.hotel.model.enums.UserRole;
import com.hotel.repository.*;
import com.hotel.service.InternalCommunicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service implementation for internal messaging and notification operations.
 * Enforces business rules, server-side validation, and security scoping.
 */
@Service
@Transactional
public class InternalCommunicationServiceImpl implements InternalCommunicationService {

    private final MessageRepository messageRepository;
    private final MessageRecipientRepository messageRecipientRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final StaffRepository staffRepository;

    @Autowired
    public InternalCommunicationServiceImpl(MessageRepository messageRepository,
                                            MessageRecipientRepository messageRecipientRepository,
                                            NotificationRepository notificationRepository,
                                            UserRepository userRepository,
                                            AdminRepository adminRepository,
                                            StaffRepository staffRepository) {
        this.messageRepository = messageRepository;
        this.messageRecipientRepository = messageRecipientRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.staffRepository = staffRepository;
    }

    @Override
    public Message sendAdminMessage(String adminUsername, List<Long> staffRecipientIds, String subject, String content) {
        if (adminUsername == null || adminUsername.isBlank()) {
            throw new IllegalArgumentException("Admin username is required.");
        }
        if (staffRecipientIds == null || staffRecipientIds.isEmpty()) {
            throw new IllegalArgumentException("At least one Staff recipient must be selected.");
        }
        if (subject == null || subject.trim().isBlank()) {
            throw new IllegalArgumentException("Subject cannot be empty.");
        }
        if (content == null || content.trim().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }

        User sender = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found: " + adminUsername));

        Message message = new Message(sender, subject.trim(), content.trim(), "General Message", UserRole.ADMIN);
        message = messageRepository.save(message);

        String preview = generatePreview(content.trim());

        List<Staff> selectedStaff = staffRepository.findAllById(staffRecipientIds);
        if (selectedStaff.isEmpty()) {
            throw new IllegalArgumentException("None of the selected staff members could be found.");
        }

        for (Staff staff : selectedStaff) {
            MessageRecipient recipientRecord = new MessageRecipient(message, staff);
            messageRecipientRepository.save(recipientRecord);
            message.getRecipients().add(recipientRecord);

            Notification notification = new Notification(
                    staff,
                    sender,
                    message,
                    subject.trim(),
                    preview,
                    "ADMIN_MESSAGE"
            );
            notificationRepository.save(notification);
        }

        return message;
    }

    @Override
    public Message sendStaffRequest(String staffUsername, String requestType, String subject, String content) {
        if (staffUsername == null || staffUsername.isBlank()) {
            throw new IllegalArgumentException("Staff username is required.");
        }
        if (requestType == null || requestType.trim().isBlank()) {
            throw new IllegalArgumentException("Request type is required.");
        }
        if (subject == null || subject.trim().isBlank()) {
            throw new IllegalArgumentException("Subject cannot be empty.");
        }
        if (content == null || content.trim().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }

        User sender = userRepository.findByUsername(staffUsername)
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found: " + staffUsername));

        Message message = new Message(sender, subject.trim(), content.trim(), requestType.trim(), UserRole.STAFF);
        message = messageRepository.save(message);

        String preview = generatePreview(content.trim());

        // Requirement 6: Staff messages MUST be delivered to ALL active Admin users.
        List<Admin> allAdmins = adminRepository.findAll();
        List<Admin> activeAdmins = allAdmins.stream()
                .filter(a -> a.isActive() && (a.getStatus() == null || a.getStatus() == AccountStatus.ACTIVE))
                .toList();

        if (activeAdmins.isEmpty()) {
            activeAdmins = allAdmins; // Fallback if status flag is unassigned
        }

        for (Admin admin : activeAdmins) {
            MessageRecipient recipientRecord = new MessageRecipient(message, admin);
            messageRecipientRepository.save(recipientRecord);
            message.getRecipients().add(recipientRecord);

            Notification notification = new Notification(
                    admin,
                    sender,
                    message,
                    subject.trim(),
                    preview,
                    "STAFF_REQUEST"
            );
            notificationRepository.save(notification);
        }

        return message;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getInboxMessages(String username) {
        if (username == null || username.isBlank()) {
            return Collections.emptyList();
        }
        return messageRepository.findReceivedMessagesByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getSentMessages(String username) {
        if (username == null || username.isBlank()) {
            return Collections.emptyList();
        }
        return messageRepository.findBySenderUsernameOrderBySentAtDesc(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getStaffRequestsForAdmin(String adminUsername) {
        if (adminUsername == null || adminUsername.isBlank()) {
            return Collections.emptyList();
        }
        return messageRepository.findReceivedStaffRequestsByAdminUsername(adminUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(String username) {
        if (username == null || username.isBlank()) {
            return Collections.emptyList();
        }
        return notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc(username);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        return notificationRepository.countByRecipientUsernameAndIsReadFalse(username);
    }

    @Override
    public Message getMessageForUser(Long messageId, String username) {
        if (messageId == null || username == null) {
            throw new IllegalArgumentException("Message ID and username are required.");
        }

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found with ID: " + messageId));

        boolean isSender = message.getSender() != null &&
                message.getSender().getUsername().equalsIgnoreCase(username);

        Optional<MessageRecipient> recipientOpt = messageRecipientRepository
                .findByMessageIdAndRecipientUsername(messageId, username);

        if (!isSender && recipientOpt.isEmpty()) {
            throw new AccessDeniedException("Access denied: You are neither the sender nor a recipient of this message.");
        }

        // If the viewer is a recipient and it is currently unread, mark as read
        if (recipientOpt.isPresent()) {
            MessageRecipient recipientRecord = recipientOpt.get();
            if (!recipientRecord.isRead()) {
                recipientRecord.setRead(true);
                recipientRecord.setReadAt(LocalDateTime.now());
                messageRecipientRepository.save(recipientRecord);

                // Also update corresponding notification
                notificationRepository.findByMessageIdAndRecipientUsername(messageId, username)
                        .ifPresent(notif -> {
                            notif.setRead(true);
                            notif.setReadAt(LocalDateTime.now());
                            notificationRepository.save(notif);
                        });
            }
        }

        return message;
    }

    @Override
    public Notification getNotificationForUser(Long notificationId, String username) {
        if (notificationId == null || username == null) {
            throw new IllegalArgumentException("Notification ID and username are required.");
        }

        Notification notification = notificationRepository.findByIdAndRecipientUsername(notificationId, username)
                .orElseThrow(() -> new AccessDeniedException("Notification not found or access denied."));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);

            // Also mark corresponding message recipient record as read
            if (notification.getMessage() != null) {
                messageRecipientRepository.findByMessageIdAndRecipientUsername(notification.getMessage().getId(), username)
                        .ifPresent(mr -> {
                            mr.setRead(true);
                            mr.setReadAt(LocalDateTime.now());
                            messageRecipientRepository.save(mr);
                        });
            }
        }

        return notification;
    }

    @Override
    public void markAllNotificationsAsRead(String username) {
        if (username == null || username.isBlank()) return;

        List<Notification> unreadList = notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc(username);
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : unreadList) {
            if (!n.isRead()) {
                n.setRead(true);
                n.setReadAt(now);
                notificationRepository.save(n);
            }
        }

        List<MessageRecipient> unreadRecipients = messageRecipientRepository.findByRecipientUsernameAndIsReadFalse(username);
        for (MessageRecipient mr : unreadRecipients) {
            mr.setRead(true);
            mr.setReadAt(now);
            messageRecipientRepository.save(mr);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMessageReadByRecipient(Long messageId, String username) {
        if (messageId == null || username == null) return false;
        return messageRecipientRepository.findByMessageIdAndRecipientUsername(messageId, username)
                .map(MessageRecipient::isRead)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getUnreadMessageIdsForUser(String username) {
        if (username == null || username.isBlank()) return Collections.emptySet();
        List<MessageRecipient> unread = messageRecipientRepository.findByRecipientUsernameAndIsReadFalse(username);
        Set<Long> set = new HashSet<>();
        for (MessageRecipient r : unread) {
            if (r.getMessage() != null) {
                set.add(r.getMessage().getId());
            }
        }
        return set;
    }

    private String generatePreview(String content) {
        if (content == null) return "";
        content = content.replaceAll("\\s+", " ").trim();
        return content.length() > 120 ? content.substring(0, 117) + "..." : content;
    }
}
