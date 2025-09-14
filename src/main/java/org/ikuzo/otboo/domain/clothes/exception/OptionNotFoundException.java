package org.ikuzo.otboo.domain.clothes.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;

public class OptionNotFoundException extends ClothesException {

    public OptionNotFoundException(String message) {
        super(ErrorCode.OPTION_VALUE_NOT_FOUND);
        this.addDetail("message", message);
    }
}
