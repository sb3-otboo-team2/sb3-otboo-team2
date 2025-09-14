package org.ikuzo.otboo.domain.clothes.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDto;
import org.ikuzo.otboo.domain.clothes.entity.ClothesType;

public record ClothesUpdateRequest(
    @NotNull
    String name,

    @NotNull
    ClothesType type,

    @Valid
    List<ClothesAttributeDto> attributes
) {

}
