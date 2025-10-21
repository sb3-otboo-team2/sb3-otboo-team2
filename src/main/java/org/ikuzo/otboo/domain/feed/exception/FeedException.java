package org.ikuzo.otboo.domain.feed.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;
import org.ikuzo.otboo.global.exception.OtbooException;

public class FeedException extends OtbooException {

    protected FeedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
