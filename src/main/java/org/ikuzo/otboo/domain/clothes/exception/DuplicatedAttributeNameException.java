package org.ikuzo.otboo.domain.clothes.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;

public class DuplicatedAttributeNameException extends ClothesException {

    public DuplicatedAttributeNameException() {
        super(ErrorCode.DUPLICATED_ATTRIBUTE_NAME);
    }
}
