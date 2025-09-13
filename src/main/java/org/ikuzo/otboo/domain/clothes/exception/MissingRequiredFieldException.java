package org.ikuzo.otboo.domain.clothes.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;

public class MissingRequiredFieldException extends ClothesException {

    public MissingRequiredFieldException(String message) {
        super(ErrorCode.REQUIRED_FIELD_MISSING);
        this.addDetail("message", message);
    }
}
