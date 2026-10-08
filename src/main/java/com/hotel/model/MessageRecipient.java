package com.hotel.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Domain entity representing recipient status (read/unread, read timestamp)
 * for internal messages.
 */
@Entity
@Table(name = "internal_message_recipients",
       uniqueConstraints = @UniqueConstraint(name = "uk_message_recipient", columnNames = {"message_id", "recipient_id"}))
public class MessageRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column
    private LocalDateTime readAt;

    public MessageRecipient() {
        this.isRead = false;
    }

    public MessageRecipient(Message message, User recipient) {
        this();
        this.message = message;
        this.recipient = recipient;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
