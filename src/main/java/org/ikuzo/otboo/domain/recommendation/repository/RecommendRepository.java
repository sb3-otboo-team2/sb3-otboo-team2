package org.ikuzo.otboo.domain.recommendation.repository;

import java.util.UUID;
import org.ikuzo.otboo.domain.recommendation.entity.Recommend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendRepository extends JpaRepository<Recommend, UUID> {

}
