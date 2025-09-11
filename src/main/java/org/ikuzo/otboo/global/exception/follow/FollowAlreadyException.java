package org.ikuzo.otboo.global.exception.follow;

import org.ikuzo.otboo.global.exception.ErrorCode;

import java.util.UUID;

public class FollowAlreadyException extends FollowException {
    public FollowAlreadyException() {
        super(ErrorCode.FOLLOW_ALREADY_EXISTS);
    }

    public static FollowAlreadyException alreadyException(UUID uuid) {
        FollowAlreadyException exception =  new FollowAlreadyException();
        exception.addDetail("id", uuid.toString());
        return exception;
    }
}
