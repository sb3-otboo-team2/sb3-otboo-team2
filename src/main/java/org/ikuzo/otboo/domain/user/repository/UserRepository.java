package org.ikuzo.otboo.domain.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ikuzo.otboo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID>, UserCustomRepository {
    Optional<User> findByName(String name);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByLockedFalseAndLatitudeIsNotNullAndLongitudeIsNotNull();

    @Query("SELECT u.id FROM User u WHERE u.locked = false")
    List<UUID> findUserIdsByLockedFalse();

}
