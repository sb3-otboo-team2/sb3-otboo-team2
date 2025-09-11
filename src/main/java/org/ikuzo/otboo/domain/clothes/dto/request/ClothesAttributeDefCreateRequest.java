package org.ikuzo.otboo.domain.clothes.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ClothesAttributeDefCreateRequest(

    @NotNull(message = "속성 이름이 비어있습니다")
    String name,

    @NotEmpty(message = "속성의 옵션이 비어있습니다")
    List<String> selectableValues
) {

}
