package org.ikuzo.otboo.domain.user.dto;

import java.time.Instant;
import java.util.UUID;
import org.ikuzo.otboo.domain.user.entity.Role;

public record UserDto(
    UUID id,
    Instant createdAt,
    String email,
    String name,
    Role role,
    Boolean locked
) {

}
