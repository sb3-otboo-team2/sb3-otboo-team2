package org.ikuzo.otboo.domain.user.dto;

import org.ikuzo.otboo.domain.user.entity.Role;

public record UserRoleUpdateRequest(
    Role role
) {

}
