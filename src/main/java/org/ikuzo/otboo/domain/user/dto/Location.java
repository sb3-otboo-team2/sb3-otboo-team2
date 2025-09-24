package org.ikuzo.otboo.domain.user.dto;

import java.util.List;

public record Location(
    Double latitude,
    Double longitude,
    Integer x,
    Integer y,
    List<String> locationNames
) {

}
