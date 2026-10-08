package com.hotel.service;

import com.hotel.model.Message;
import com.hotel.model.Notification;
import java.util.List;

/**
 * Service interface for internal communication and notifications between Staff and Admins.
 * Demonstrates OOP Abstraction via Java interfaces.
 */
public interface InternalCommunicationService {

    Message sendAdminMessage(String adminUsername, List<Long> staffRecipientIds, String subject, String content);

    Message sendStaffRequest(String staffUsername, String requestType, String subject, String content);

    List<Message> getInboxMessages(String username);

    List<Message> getSentMessages(String username);

    List<Message> getStaffRequestsForAdmin(String adminUsername);

    List<Notification> getNotificationsForUser(String username);

    long getUnreadNotificationCount(String username);

    Message getMessageForUser(Long messageId, String username);

    Notification getNotificationForUser(Long notificationId, String username);

    void markAllNotificationsAsRead(String username);

    boolean isMessageReadByRecipient(Long messageId, String username);

    java.util.Set<Long> getUnreadMessageIdsForUser(String username);
}
