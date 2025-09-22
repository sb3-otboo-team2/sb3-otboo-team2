package org.ikuzo.otboo.domain.clothes.infrastructure;

import java.util.List;

public record ExtractClothes(
    String name,
    String imageUrl, String type,
    List<ExtractedAttribute> attributes
) {
    public record ExtractedAttribute(String definitionName, String value) {}
}
