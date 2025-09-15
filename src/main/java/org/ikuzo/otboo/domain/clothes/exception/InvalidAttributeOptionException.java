package org.ikuzo.otboo.domain.clothes.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;

public class InvalidAttributeOptionException extends ClothesException {

    public InvalidAttributeOptionException(String message) {
        super(ErrorCode.INVALID_ATTRIBUTE_OPTION);
        this.addDetail("message", message);
    }
}
