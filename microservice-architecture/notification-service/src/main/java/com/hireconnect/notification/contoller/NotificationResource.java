package com.hireconnect.notification.contoller;

import com.hireconnect.notification.entity.Notification;
import com.hireconnect.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationResource {

    private final NotificationService notifService;

    public NotificationResource(NotificationService notifService) {
        this.notifService = notifService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> send(@Valid @RequestBody Notification notify) {
        notifService.sendNotification(notify);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Notification saved and email dispatched successfully.");
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getByUser(@PathVariable int userId) {
        return ResponseEntity.ok(notifService.getByUser(userId));
    }

    @PatchMapping("/read/{id}")
    public ResponseEntity<String> markRead(@PathVariable int id) {
        notifService.markAsRead(id);
        return ResponseEntity.ok("Notification ID " + id + " marked as read.");
    }

    @PatchMapping("/read-all/{userId}")
    public ResponseEntity<String> markAllRead(@PathVariable int userId) {
        notifService.markAllRead(userId);
        return ResponseEntity.ok("All notifications for user " + userId + " marked as read.");
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Notification>> getByType(@PathVariable String type) {
        return ResponseEntity.ok(notifService.getByType(type));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable int id) {
        notifService.deleteNotification(id);
        return ResponseEntity.ok("Notification ID " + id + " deleted successfully.");
    }

    @GetMapping("/unread-count/{userId}")
    public ResponseEntity<Long> getUnreadCount(@PathVariable int userId) {
        return ResponseEntity.ok(notifService.getUnreadCount(userId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notifService.getAllNotifications());
    }
}