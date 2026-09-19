package project.project.DTO.notification;

import project.project.Entity.notification.Notification;
import project.project.Entity.notification.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        String title,
        String message,
        NotificationType type,
        boolean isRead,
        LocalDateTime createdAt) {
    public static NotificationResponse fromEntity(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                Boolean.TRUE.equals(notification.getIsRead()),
                notification.getCreatedAt());
    }
}