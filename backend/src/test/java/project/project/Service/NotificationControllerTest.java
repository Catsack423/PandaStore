package project.project.Service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import project.project.Controller.GlobalExceptionHandler;
import project.project.Controller.NotificationController;
import project.project.Controller.RequestUserResolver;
import project.project.Entity.notification.Notification;
import project.project.Entity.notification.NotificationType;
import project.project.Service.api.NotificationService;
import project.project.Service.api.ResourceOwnershipService;

import java.security.Principal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    NotificationService notificationService;

    @Mock
    RequestUserResolver userResolver;

    @Mock
    ResourceOwnershipService ownershipService;

    private MockMvc mvc;
    private final Principal principal = () -> "customer1";

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                new NotificationController(
                        notificationService,
                        userResolver,
                        ownershipService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getOwnNotifications_returns200AndDto() throws Exception {
        when(userResolver.requireUserId(principal)).thenReturn(1L);

        Notification notification = new Notification();
        notification.setNotificationId(10L);
        notification.setTitle("Payment received");
        notification.setMessage("Order paid");
        notification.setType(NotificationType.PAYMENT_SUCCESS);
        notification.setIsRead(false);

        when(notificationService.getUserNotifications(1L))
                .thenReturn(List.of(notification));

        mvc.perform(
                get("/api/notifications/users/1")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].notificationId").value(10))
                .andExpect(jsonPath("$.data[0].type").value("PAYMENT_SUCCESS"))
                .andExpect(jsonPath("$.data[0].isRead").value(false))
                .andExpect(jsonPath("$.data[0].recipientUser").doesNotExist());

        verify(notificationService).getUserNotifications(1L);
    }

    @Test
    void getOtherUsersNotifications_returns403WithoutReading()
            throws Exception {

        when(userResolver.requireUserId(principal)).thenReturn(1L);

        mvc.perform(
                get("/api/notifications/users/2")
                        .principal(principal))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(notificationService);
    }

    @Test
    void getNotificationsWithoutLogin_returns401() throws Exception {
        when(userResolver.requireUserId(null))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Login required"));

        mvc.perform(get("/api/notifications/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(notificationService, ownershipService);
    }

    @Test
    void markAsRead_checksOwnershipBeforeUpdating() throws Exception {
        when(userResolver.requireUserId(principal)).thenReturn(1L);

        mvc.perform(
                patch("/api/notifications/10/read")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        var ordered = inOrder(ownershipService, notificationService);

        ordered.verify(ownershipService)
                .requireNotificationOwner(1L, 10L);

        ordered.verify(notificationService).markAsRead(10L);
    }

    @Test
    void markAsReadWhenNotOwner_returns404WithoutUpdating()
            throws Exception {

        when(userResolver.requireUserId(principal)).thenReturn(1L);

        doThrow(new EntityNotFoundException("Notification not found"))
                .when(ownershipService)
                .requireNotificationOwner(1L, 10L);

        mvc.perform(
                patch("/api/notifications/10/read")
                        .principal(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.message").value("Notification not found"));

        verifyNoInteractions(notificationService);
    }
}