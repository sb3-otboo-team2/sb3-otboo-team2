package org.ikuzo.otboo.domain.user.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ProfileDto(
    UUID userId,
    String name,
    String gender,
    LocalDate birthDate,
    Location location,
    Integer temperatureSensitivity,
    String profileImageUrl
) {

}
