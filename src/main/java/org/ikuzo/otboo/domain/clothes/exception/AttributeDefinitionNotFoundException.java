package org.ikuzo.otboo.domain.clothes.exception;

import java.util.UUID;
import org.ikuzo.otboo.global.exception.ErrorCode;

public class AttributeDefinitionNotFoundException extends ClothesException {

    public AttributeDefinitionNotFoundException(UUID definitionId) {
        super(ErrorCode.ATTRIBUTE_NOT_FOUND);
        this.addDetail("definitionId", definitionId);
    }
}
