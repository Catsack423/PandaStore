package project.project.Service;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import project.project.Entity.notification.Notification;
import project.project.Entity.notification.NotificationType;
import project.project.Entity.user.User;
import project.project.Service.implement.NotificationServiceImp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EntityManager entityManager;

    private NotificationServiceImp service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImp(entityManager);
    }

    @Test
    void sendNotificationShouldSaveUnreadNotification() {
        User recipient = new User();
        recipient.setUserId(10L);

        when(entityManager.find(User.class, 10L))
                .thenReturn(recipient);

        Notification result = service.sendNotification(
                10L,
                "มีใบสมัครร้านค้าใหม่",
                "ใบสมัครรอการตรวจสอบ",
                NotificationType.NEW_SELLER_APPLICATION);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        verify(entityManager).persist(captor.capture());

        Notification saved = captor.getValue();

        assertSame(result, saved);
        assertSame(recipient, saved.getRecipientUser());
        assertEquals(
                NotificationType.NEW_SELLER_APPLICATION,
                saved.getType());
        assertEquals(Boolean.FALSE, saved.getIsRead());
        assertEquals("มีใบสมัครร้านค้าใหม่", saved.getTitle());
    }

    @Test
    void sendNotificationShouldRejectBlankTitle() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.sendNotification(
                        10L,
                        " ",
                        "ข้อความ",
                        NotificationType.NEW_SELLER_APPLICATION));

        verify(entityManager, never()).persist(any());
    }

    @Test
    void markAsReadShouldAllowOwner() {
        User owner = new User();
        owner.setUserId(10L);

        Notification notification = new Notification();
        notification.setRecipientUser(owner);
        notification.setIsRead(false);

        when(entityManager.find(Notification.class, 100L))
                .thenReturn(notification);

        service.markAsRead(10L, 100L);

        assertEquals(Boolean.TRUE, notification.getIsRead());
    }

    @Test
    void markAsReadShouldRejectAnotherUser() {
        User owner = new User();
        owner.setUserId(10L);

        Notification notification = new Notification();
        notification.setRecipientUser(owner);
        notification.setIsRead(false);

        when(entityManager.find(Notification.class, 100L))
                .thenReturn(notification);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.markAsRead(20L, 100L));

        assertEquals(Boolean.FALSE, notification.getIsRead());
    }
}