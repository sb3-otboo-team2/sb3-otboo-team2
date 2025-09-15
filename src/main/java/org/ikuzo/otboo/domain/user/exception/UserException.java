package org.ikuzo.otboo.domain.user.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;
import org.ikuzo.otboo.global.exception.OtbooException;

public class UserException extends OtbooException {
    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}