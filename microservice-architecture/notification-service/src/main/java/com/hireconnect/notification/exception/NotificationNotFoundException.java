package com.hireconnect.notification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * STEP 2 — CUSTOM EXCEPTION
 *
 * Why this exists:
 * Before this, the code threw a raw RuntimeException("Notification not found").
 * That causes Spring to return HTTP 500 (Internal Server Error) — which is wrong.
 * A missing record is the CLIENT's problem, not the server's.
 *
 * Fix:
 * ✅ @ResponseStatus(HttpStatus.NOT_FOUND) makes Spring automatically return HTTP 404
 *    whenever this exception is thrown — no extra code needed in the controller.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(int id) {
        super("Notification with ID " + id + " not found");
    }
}