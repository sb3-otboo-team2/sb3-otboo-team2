package org.ikuzo.otboo.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 6, max = 60, message = "비밀번호는 6자 이상 60자 이하여야 합니다")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z]).{6,}$",
        message = "비밀번호는 최소 6자 이상, 숫자와 문자를 포함해야 합니다")
    String password
) {

}
