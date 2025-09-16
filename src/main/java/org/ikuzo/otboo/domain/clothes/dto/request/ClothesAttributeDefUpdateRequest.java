package org.ikuzo.otboo.domain.clothes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ClothesAttributeDefUpdateRequest(
    @NotBlank(message = "속성 이름이 비어있습니다")
    @Size(max = 50, message = "속성 이름은 50자 이하여야 합니다")
    String name,

    @NotEmpty(message = "속성의 옵션이 비어있습니다")
    List<String> selectableValues
) {

}
