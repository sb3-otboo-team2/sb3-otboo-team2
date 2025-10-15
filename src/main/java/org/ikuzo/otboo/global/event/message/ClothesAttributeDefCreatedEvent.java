package org.ikuzo.otboo.global.event.message;

import java.time.Instant;
import lombok.Getter;
import org.ikuzo.otboo.domain.clothes.dto.ClothesAttributeDefDto;

@Getter
public class ClothesAttributeDefCreatedEvent extends CreatedEvent<ClothesAttributeDefDto> {

    public ClothesAttributeDefCreatedEvent(ClothesAttributeDefDto dto, Instant createdAt) {
        super(dto, createdAt);
    }
}
