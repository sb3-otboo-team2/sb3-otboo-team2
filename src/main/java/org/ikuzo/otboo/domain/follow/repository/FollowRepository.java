package org.ikuzo.otboo.domain.follow.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ikuzo.otboo.domain.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID>, FollowCustomRepository {

    boolean existsByFollower_IdAndFollowing_Id(UUID followerId, UUID followingId);

    long countByFollowing_Id(UUID userId);

    long countByFollower_Id(UUID userId);

    Optional<Follow> findByFollower_IdAndFollowing_Id(UUID followerId, UUID followingId);

    @Query("select f.follower.id from Follow f where f.following.id = :followingId")
    List<UUID> findFollowerIdsByFollowingId(@Param("followingId") UUID followingId);
}
