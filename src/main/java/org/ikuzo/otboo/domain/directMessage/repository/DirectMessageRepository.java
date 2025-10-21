package org.ikuzo.otboo.domain.directMessage.repository;

import org.ikuzo.otboo.domain.directMessage.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID>, DirectMessageCustomRepository {
}
