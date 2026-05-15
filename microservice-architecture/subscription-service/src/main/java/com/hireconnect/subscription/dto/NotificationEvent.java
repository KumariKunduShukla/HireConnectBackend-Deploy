package com.hireconnect.subscription.dto;

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
}
