package com.hotel.model;

import com.hotel.model.enums.UserRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity representing internal communications between Staff and Administrators.
 * Demonstrates OOP Encapsulation and multi-recipient association mapping.
 */
@Entity
@Table(name = "internal_messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 150)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 50)
    private String requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole senderRole;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageRecipient> recipients = new ArrayList<>();

    public Message() {
        this.sentAt = LocalDateTime.now();
    }

    public Message(User sender, String subject, String content, String requestType, UserRole senderRole) {
        this();
        this.sender = sender;
        this.subject = subject;
        this.content = content;
        this.requestType = requestType;
        this.senderRole = senderRole;
    }

    public void addRecipient(MessageRecipient recipient) {
        recipients.add(recipient);
        recipient.setMessage(this);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public UserRole getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(UserRole senderRole) {
        this.senderRole = senderRole;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public List<MessageRecipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<MessageRecipient> recipients) {
        this.recipients = recipients;
    }
}
