package org.ikuzo.otboo.domain.clothes.mapper;

import java.util.List;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeWithDefDto;
import org.ikuzo.otboo.domain.clothes.dto.ClothesDto;
import org.ikuzo.otboo.domain.clothes.entity.AttributeOption;
import org.ikuzo.otboo.domain.clothes.entity.Clothes;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttribute;
import org.ikuzo.otboo.domain.clothes.entity.ClothesAttributeDef;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ClothesMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "attributes", source = "attributes", qualifiedByName = "mapAttributes")
    ClothesDto toDto(Clothes clothes);

    @Named("mapAttributes")
    default List<ClothesAttributeWithDefDto> mapAttributes(List<ClothesAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return List.of();
        }
        return attributes.stream()
            .map(attribute -> {
                ClothesAttributeDef def = attribute.getDefinition();
                List<AttributeOption> options = def.getOptions();
                List<String> selectable = (options == null)
                    ? List.of()
                    : options.stream().map(AttributeOption::getValue).toList();
                return new ClothesAttributeWithDefDto(
                    def.getId(),
                    def.getName(),
                    selectable,
                    attribute.getOptionValue()
                );
            })
            .toList();
    }

}
