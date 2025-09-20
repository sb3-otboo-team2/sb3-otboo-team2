package org.ikuzo.otboo.domain.feed.repository;

import java.util.UUID;
import org.ikuzo.otboo.domain.feed.entity.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedRepository extends JpaRepository<Feed, UUID> {
}