package org.ikuzo.otboo.domain.directMessage.repository;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.directMessage.entity.DirectMessage;
import org.ikuzo.otboo.domain.directMessage.entity.QDirectMessage;
import org.ikuzo.otboo.domain.user.entity.QUser;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DirectMessageCustomRepositoryImpl implements DirectMessageCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<DirectMessage> getDirectMessages(UUID currentId, UUID userId, Instant cursor, UUID idAfter, int limit) {
        QDirectMessage directMessage = QDirectMessage.directMessage;
        QUser sender = new QUser("sender");
        QUser receiver = new QUser("receiver");

        JPAQuery<DirectMessage> query = queryFactory.selectFrom(directMessage);

        query
            .join(directMessage.sender, sender).fetchJoin()
            .join(directMessage.receiver, receiver).fetchJoin()
            .where(
                (directMessage.sender.id.eq(userId).and(directMessage.receiver.id.eq(currentId)))
                    .or(directMessage.sender.id.eq(currentId).and(directMessage.receiver.id.eq(userId)))
            );

        if (cursor != null && idAfter != null) {
            query.where(
                directMessage.createdAt.lt(cursor)
                    .or(directMessage.createdAt.eq(cursor)
                        .and(directMessage.id.lt(idAfter)))
            );
        }

        return query
            .orderBy(directMessage.createdAt.desc())
            .limit(limit + 1)
            .fetch();
    }

    @Override
    public long countDirectMessages(UUID currentId, UUID userId) {
        QDirectMessage directMessage = QDirectMessage.directMessage;

        return queryFactory
            .select(directMessage.count())
            .from(directMessage)
            .where(
                (directMessage.sender.id.eq(userId).and(directMessage.receiver.id.eq(currentId)))
                    .or(directMessage.sender.id.eq(currentId).and(directMessage.receiver.id.eq(userId)))
            )
            .fetchFirst();
    }
}
