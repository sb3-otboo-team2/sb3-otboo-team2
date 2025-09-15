package org.ikuzo.otboo.domain.follow.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;

import java.util.UUID;

public class FollowSelfNotAllowException extends FollowException {
    public FollowSelfNotAllowException() {
        super(ErrorCode.FOLLOW_SELF_NOT_ALLOWED);
    }

    public static FollowSelfNotAllowException notAllowFollowSelf(UUID uuid) {
        FollowSelfNotAllowException exception = new FollowSelfNotAllowException();
        exception.addDetail("followSelf", uuid.toString());
        return exception;
    }
}
