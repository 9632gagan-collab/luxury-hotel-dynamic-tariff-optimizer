package com.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Admin sending internal message to selected Staff.
 */
public class AdminSendMessageDto {

    @NotEmpty(message = "Please select at least one Staff recipient.")
    private List<Long> staffRecipientIds = new ArrayList<>();

    @NotBlank(message = "Subject is required.")
    @Size(min = 3, max = 150, message = "Subject must be between 3 and 150 characters.")
    private String subject;

    @NotBlank(message = "Message content is required.")
    @Size(min = 5, max = 3000, message = "Message content must be between 5 and 3000 characters.")
    private String content;

    public AdminSendMessageDto() {
    }

    public AdminSendMessageDto(List<Long> staffRecipientIds, String subject, String content) {
        this.staffRecipientIds = staffRecipientIds != null ? staffRecipientIds : new ArrayList<>();
        this.subject = subject;
        this.content = content;
    }

    public List<Long> getStaffRecipientIds() {
        return staffRecipientIds;
    }

    public void setStaffRecipientIds(List<Long> staffRecipientIds) {
        this.staffRecipientIds = staffRecipientIds != null ? staffRecipientIds : new ArrayList<>();
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
