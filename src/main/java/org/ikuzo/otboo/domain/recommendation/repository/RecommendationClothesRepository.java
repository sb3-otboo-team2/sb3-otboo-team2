package org.ikuzo.otboo.domain.recommendation.repository;

import java.util.UUID;
import org.ikuzo.otboo.domain.recommendation.entity.RecommendationClothes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationClothesRepository extends JpaRepository<RecommendationClothes, UUID> {

}
