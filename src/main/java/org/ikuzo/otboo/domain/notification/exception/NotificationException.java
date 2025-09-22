package org.ikuzo.otboo.domain.notification.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;
import org.ikuzo.otboo.global.exception.OtbooException;

public class NotificationException extends OtbooException {
    public NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
