package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.notification.Notification;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUser_UserIdOrderByCreatedAtDescNotificationIdDesc(Long userId);
}