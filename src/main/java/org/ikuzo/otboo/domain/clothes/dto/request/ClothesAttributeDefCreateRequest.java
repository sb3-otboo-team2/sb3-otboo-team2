package org.ikuzo.otboo.domain.clothes.dto.request;

import java.util.List;

public record ClothesAttributeDefCreateRequest(
    String name,
    List<String> selectableValues
) {

}
