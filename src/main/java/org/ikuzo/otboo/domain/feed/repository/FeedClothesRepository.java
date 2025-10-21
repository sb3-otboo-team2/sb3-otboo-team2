package org.ikuzo.otboo.domain.feed.repository;

import java.util.UUID;
import org.ikuzo.otboo.domain.feed.entity.FeedClothes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedClothesRepository extends JpaRepository<FeedClothes, UUID> {
}