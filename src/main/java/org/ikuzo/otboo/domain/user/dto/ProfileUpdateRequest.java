package org.ikuzo.otboo.domain.user.dto;

import java.time.LocalDate;
import org.ikuzo.otboo.domain.user.entity.Gender;

public record ProfileUpdateRequest(
    String name,
    Gender gender,
    LocalDate birthDate,
    Location location,
    Integer temperatureSensitivity
) {

}
