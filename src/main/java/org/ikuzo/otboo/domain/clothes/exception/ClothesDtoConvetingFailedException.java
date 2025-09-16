package org.ikuzo.otboo.domain.clothes.exception;

import java.util.UUID;
import org.ikuzo.otboo.global.exception.ErrorCode;

public class ClothesDtoConvetingFailedException extends ClothesException {

    public ClothesDtoConvetingFailedException(UUID ClothesId) {
        super(ErrorCode.CLOTHING_MAPPER_CONVERSION_FAILED);
        this.addDetail("clothesId", ClothesId);
    }
}
