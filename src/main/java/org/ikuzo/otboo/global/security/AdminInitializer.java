package org.ikuzo.otboo.global.security;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.auth.service.AuthService;
import org.ikuzo.otboo.domain.user.dto.UserCreateRequest;
import org.ikuzo.otboo.domain.user.dto.UserDto;
import org.ikuzo.otboo.domain.user.dto.UserRoleUpdateRequest;
import org.ikuzo.otboo.domain.user.entity.Role;
import org.ikuzo.otboo.domain.user.exception.UserAlreadyExistsException;
import org.ikuzo.otboo.domain.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AdminInitializer implements ApplicationRunner {

  @Value("${admin.name}")
  private String name;
  @Value("${admin.email}")
  private String email;
  @Value("${admin.password}")
  private String password;

  private final UserService userService;

  @Override
  public void run(ApplicationArguments args) {
    UserCreateRequest request = new UserCreateRequest(name, email, password);
    try {
      UserDto admin = userService.create(request);
      userService.updateRoleInternal(admin.id(), new UserRoleUpdateRequest(Role.ADMIN));
      log.info("관리자 계정이 성공적으로 생성되었습니다.");
    } catch (UserAlreadyExistsException e) {
      log.warn("관리자 계정이 이미 존재합니다");
    } catch (Exception e) {
      log.error("관리자 계정 생성 중 오류가 발생했습니다.: {}", e.getMessage());
    }
  }
}
