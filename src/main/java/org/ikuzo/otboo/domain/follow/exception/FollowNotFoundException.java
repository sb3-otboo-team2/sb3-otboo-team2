package org.ikuzo.otboo.domain.follow.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;

import java.util.UUID;

public class FollowNotFoundException extends FollowException {
    public FollowNotFoundException() {
        super(ErrorCode.FOLLOW_NOT_FOUND);
    }

    public static FollowNotFoundException notFoundException(UUID uuid) {
        FollowNotFoundException exception = new FollowNotFoundException();
        exception.addDetail("id", uuid.toString());
        return exception;
    }
}
