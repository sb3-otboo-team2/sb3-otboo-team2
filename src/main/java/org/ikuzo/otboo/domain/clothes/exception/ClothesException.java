package org.ikuzo.otboo.domain.clothes.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;
import org.ikuzo.otboo.global.exception.OtbooException;

public class ClothesException extends OtbooException {

    public ClothesException(ErrorCode errorCode) {
        super(errorCode);
    }
}
