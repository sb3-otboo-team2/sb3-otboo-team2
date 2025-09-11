package org.ikuzo.otboo.domain.feedclothes.repository;

import java.util.UUID;
import org.ikuzo.otboo.domain.feedclothes.entity.FeedClothes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedClothesRepository extends JpaRepository<FeedClothes, UUID> {
}