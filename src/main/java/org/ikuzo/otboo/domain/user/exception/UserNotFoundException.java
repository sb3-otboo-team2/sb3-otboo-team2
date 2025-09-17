package org.ikuzo.otboo.domain.user.exception;

import java.util.UUID;
import org.ikuzo.otboo.global.exception.ErrorCode;

public class UserNotFoundException extends UserException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
    
    public static UserNotFoundException withId(UUID userId) {
        UserNotFoundException exception = new UserNotFoundException();
        exception.addDetail("userId", userId);
        return exception;
    }
    
    public static UserNotFoundException withEmail(String email) {
        UserNotFoundException exception = new UserNotFoundException();
        exception.addDetail("email", email);
        return exception;
    }
} 