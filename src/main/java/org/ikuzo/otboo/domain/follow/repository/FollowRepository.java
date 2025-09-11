package org.ikuzo.otboo.domain.follow.repository;

import org.ikuzo.otboo.domain.follow.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsByFollower_IdAndFollowing_Id(UUID followerId, UUID followingId);
}
