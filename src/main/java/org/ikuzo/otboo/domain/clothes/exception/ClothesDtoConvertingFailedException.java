package org.ikuzo.otboo.domain.clothes.exception;

import java.util.UUID;
import org.ikuzo.otboo.global.exception.ErrorCode;

public class ClothesDtoConvertingFailedException extends ClothesException {

    public ClothesDtoConvertingFailedException(UUID clothesId) {
        super(ErrorCode.CLOTHING_MAPPER_CONVERSION_FAILED);
        this.addDetail("clothesId", clothesId);
    }
}
