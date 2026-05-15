package com.hireconnect.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent {
    private int userId;
    private String recipientEmail;
    private String type;
    private String message;
    private byte[] attachmentBytes;
    private String attachmentName;

    public NotificationEvent(int userId, String recipientEmail, String type, String message) {
        this.userId = userId;
        this.recipientEmail = recipientEmail;
        this.type = type;
        this.message = message;
    }
}
