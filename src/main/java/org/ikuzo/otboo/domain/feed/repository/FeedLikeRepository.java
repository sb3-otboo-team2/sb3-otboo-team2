package org.ikuzo.otboo.domain.feed.repository;

import java.util.UUID;
import org.ikuzo.otboo.domain.feed.entity.FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedLikeRepository extends JpaRepository<FeedLike, UUID> {
}
