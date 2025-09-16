package org.ikuzo.otboo.domain.user.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;

public class UserAlreadyExistsException extends UserException {
    public UserAlreadyExistsException() {
        super(ErrorCode.DUPLICATE_USER);
    }
    
    public static UserAlreadyExistsException withEmail(String email) {
        UserAlreadyExistsException exception = new UserAlreadyExistsException();
        exception.addDetail("email", email);
        return exception;
    }
} 