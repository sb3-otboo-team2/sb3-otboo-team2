package org.ikuzo.otboo.domain.clothes.exception;

import org.ikuzo.otboo.global.exception.ErrorCode;

public class DuplicatedAttributeNameException extends ClothesException {

    public DuplicatedAttributeNameException(String name) {
        super(ErrorCode.DUPLICATED_ATTRIBUTE_NAME);
        this.addDetail("name", name);
    }
}
