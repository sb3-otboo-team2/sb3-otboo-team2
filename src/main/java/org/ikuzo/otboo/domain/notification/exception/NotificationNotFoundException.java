package org.ikuzo.otboo.domain.notification.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;

import java.util.UUID;

public class NotificationNotFoundException extends NotificationException {
    public NotificationNotFoundException() {
        super(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    public static NotificationNotFoundException notFoundException(UUID notificationId) {
        NotificationNotFoundException exception = new NotificationNotFoundException();
        exception.addDetail("id", notificationId);
        return exception;
    }
}