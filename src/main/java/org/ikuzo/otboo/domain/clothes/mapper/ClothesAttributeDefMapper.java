package org.ikuzo.otboo.domain.clothes.mapper;

import java.util.List;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDefDto;
import org.ikuzo.otboo.domain.clothes.entity.AttributeOption;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttributeDef;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClothesAttributeDefMapper {

    @Mapping(target = "selectableValues", source = "options")
    ClothesAttributeDefDto toDto(ClothesAttributeDef entity);

    default String map(AttributeOption option) {
        return option == null ? null : option.getValue();
    }

}
