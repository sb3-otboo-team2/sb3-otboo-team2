package org.ikuzo.otboo.domain.follow.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;
import org.ikuzo.otboo.global.exception.OtbooException;

public class FollowException extends OtbooException {
    public FollowException(ErrorCode errorCode) {
        super(errorCode);
    }
}
