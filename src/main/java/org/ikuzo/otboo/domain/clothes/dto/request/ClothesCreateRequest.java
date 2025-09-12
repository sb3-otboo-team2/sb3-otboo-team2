package org.ikuzo.otboo.domain.clothes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDto;
import org.ikuzo.otboo.domain.clothes.entity.ClothesType;

public record ClothesCreateRequest(

    @NotNull
    UUID ownerId,

    @NotBlank
    @Size(max = 100)
    String name,

    @NotNull
    ClothesType type,

    List<ClothesAttributeDto> attributes
) {

}
