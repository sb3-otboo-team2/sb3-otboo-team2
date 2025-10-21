package org.ikuzo.otboo.domain.notification.controller;

import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.notification.dto.NotificationDto;
import org.ikuzo.otboo.domain.notification.service.NotificationService;
import org.ikuzo.otboo.global.dto.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationDto>> getNotifications(
        @RequestParam(required = false) Instant cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam int limit
    ) {
        PageResponse<NotificationDto> response = notificationService.getNotifications(cursor, idAfter, limit);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
