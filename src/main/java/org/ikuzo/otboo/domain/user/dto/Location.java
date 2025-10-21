package org.ikuzo.otboo.domain.user.dto;

import java.util.List;

public record Location(
    Double latitude,
    Double longitude,
    Integer x,
    Integer y,
    List<String> locationNames
) {
    public String getLocationNamesAsString() {
        if (locationNames == null || locationNames.isEmpty()) {
            return null;
        }
        return String.join(" ", locationNames);
    }
}
