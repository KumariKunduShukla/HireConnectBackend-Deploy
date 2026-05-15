package com.hireconnect.notification.repository;

import com.hireconnect.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * STEP 4 — REPOSITORY (Database Access Layer)
 *
 * JpaRepository<Notification, Integer> gives us free CRUD methods:
 *   save(), findById(), findAll(), deleteById(), count(), etc.
 *
 * We add custom queries below for our specific business needs.
 *
 * Key fix:
 * ✅ markAllAsReadByUserId() uses a single bulk JPQL UPDATE instead of
 *    fetching ALL rows into Java memory and looping over them.
 *    For a user with 1000 unread notifications, the old way fired 1000 SQL statements.
 *    The new way fires exactly 1 SQL statement regardless of row count.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // Returns all notifications for a given user (for the notification dashboard)
    List<Notification> findByUserId(int userId);

    // Filters by user + read status (used to fetch only unread ones)
    List<Notification> findByUserIdAndIsRead(int userId, boolean isRead);

    // Filters by notification type (e.g., fetch only "Job Alert" notifications)
    List<Notification> findByType(String type);

    // Counts unread notifications (drives the red badge number on the bell icon)
    long countByUserIdAndIsRead(int userId, boolean isRead);

    /**
     * ✅ BULK UPDATE — marks ALL unread notifications as read in a single SQL statement.
     *
     * @Modifying tells Spring this query changes data (not a SELECT).
     *            Without it, Spring throws: "Not supported for DML operations".
     *
     * @Query runs raw JPQL (object-level SQL) directly —
     *        no need to load entities into memory first.
     *
     * clearAutomatically = true forces Hibernate to clear its first-level cache
     * after the update, so any subsequent findBy... calls see fresh data.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") int userId);
}