package org.ikuzo.otboo.domain.clothes.dto;

import java.util.UUID;

public record ClothesAttributeDto(
    UUID definitionId,
    String value
) {

}
