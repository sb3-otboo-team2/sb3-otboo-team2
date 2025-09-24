package org.ikuzo.otboo.domain.feedLike.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.ikuzo.otboo.domain.feedLike.entity.FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedLikeRepository extends JpaRepository<FeedLike, UUID> {

    List<FeedLike> findByUser_IdAndFeed_IdIn(UUID userId, Collection<UUID> feedIds);

    Boolean existsByUser_IdAndFeed_Id(UUID userId, UUID feedId);
}