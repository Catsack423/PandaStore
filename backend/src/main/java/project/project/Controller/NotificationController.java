package project.project.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import project.project.ApiResponse.ApiResponse;
import project.project.DTO.notification.NotificationResponse;
import project.project.Service.api.NotificationService;
import project.project.Service.api.ResourceOwnershipService;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final RequestUserResolver requestUserResolver;
    private final ResourceOwnershipService ownershipService;

    public NotificationController(
            NotificationService notificationService,
            RequestUserResolver requestUserResolver,
            ResourceOwnershipService ownershipService) {
        this.notificationService = notificationService;
        this.requestUserResolver = requestUserResolver;
        this.ownershipService = ownershipService;
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
            @PathVariable Long userId,
            Principal principal) {
        Long currentUserId = requestUserResolver.requireUserId(principal);

        if (!Objects.equals(currentUserId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "ไม่สามารถดูการแจ้งเตือนของผู้ใช้อื่นได้");
        }

        var notifications = notificationService
                .getUserNotifications(currentUserId)
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "ดึงข้อมูลการแจ้งเตือนสำเร็จ",
                        notifications));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            Principal principal) {
        Long userId = requestUserResolver.requireUserId(principal);

        ownershipService.requireNotificationOwner(userId, notificationId);
        notificationService.markAsRead(notificationId);

        return ResponseEntity.ok(
                ApiResponse.success("อ่านการแจ้งเตือนแล้ว", null));
    }
}