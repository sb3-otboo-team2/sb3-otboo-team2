package org.ikuzo.otboo.domain.clothes.exception;

import java.util.UUID;
import org.ikuzo.otboo.global.exception.ErrorCode;

public class ClothesNotFoundException extends ClothesException {

    public ClothesNotFoundException(UUID clothesId) {
        super(ErrorCode.CLOTHES_NOT_FOUND);
        this.addDetail("clothesId", clothesId);
    }
}
