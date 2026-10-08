package com.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Staff sending request/message to all Admins.
 * Note: Staff interface does NOT include an Admin recipient selection field
 * as all messages are automatically delivered to all active Admins.
 */
public class StaffSendRequestDto {

    @NotBlank(message = "Please select a request type.")
    private String requestType = "General Message";

    @NotBlank(message = "Subject is required.")
    @Size(min = 3, max = 150, message = "Subject must be between 3 and 150 characters.")
    private String subject;

    @NotBlank(message = "Message content is required.")
    @Size(min = 5, max = 3000, message = "Message content must be between 5 and 3000 characters.")
    private String content;

    public StaffSendRequestDto() {
    }

    public StaffSendRequestDto(String requestType, String subject, String content) {
        this.requestType = requestType;
        this.subject = subject;
        this.content = content;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
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
}
