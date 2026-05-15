package com.hireconnect.notification.service;

import com.hireconnect.notification.entity.Notification;
import java.util.List;

public interface NotificationService {
    void sendNotification(Notification notification);
    void sendNotification(Notification notification, byte[] attachment, String fileName);
    List<Notification> getByUser(int userId);
    List<Notification> getByType(String type);
    void markAsRead(int id);
    void markAllRead(int userId);
    void deleteNotification(int id);
    long getUnreadCount(int userId);
    List<Notification> getAllNotifications();
    void sendEmailAlert(String to, String subject, String body);
    void sendEmailAlert(String to, String subject, String body, byte[] attachment, String fileName);
}