package org.ikuzo.otboo.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
    @NotBlank(message = "사용자 이름은 필수입니다")
    @Size(min = 2, max = 20, message = "사용자 이름은 2자 이상 20자 이하여야 합니다")
    String name,

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이어야 합니다")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 6, max = 60, message = "비밀번호는 6자 이상 60자 이하여야 합니다")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z]).{6,}$",
        message = "비밀번호는 최소 6자 이상, 숫자와 문자를 포함해야 합니다")
    String password
) {

}
