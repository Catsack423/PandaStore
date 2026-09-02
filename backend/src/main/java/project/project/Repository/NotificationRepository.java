package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.notification.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

}
