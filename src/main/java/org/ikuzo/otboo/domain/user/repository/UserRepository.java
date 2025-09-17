package org.ikuzo.otboo.domain.user.repository;

import java.util.Optional;
import java.util.UUID;
import org.ikuzo.otboo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByName(String name);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
