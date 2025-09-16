package org.ikuzo.otboo.domain.user.exception;

import java.util.UUID;
import org.ikuzo.otboo.global.exception.ErrorCode;

public class UserLocationMissingException extends UserException {

    public UserLocationMissingException() {
        super(ErrorCode.USER_LOCATION_MISSING);
    }

    public static UserLocationMissingException withUserId(UUID userId) {
        UserLocationMissingException ex = new UserLocationMissingException();
        ex.addDetail("userId", userId);
        return ex;
    }
}