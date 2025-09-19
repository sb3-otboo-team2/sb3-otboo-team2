package org.ikuzo.otboo.domain.notification.repository;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.ikuzo.otboo.domain.notification.entity.Notification;
import org.ikuzo.otboo.domain.notification.entity.QNotification;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationCustomRepositoryImpl implements NotificationCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Notification> findByCursor(UUID userId, Instant cursor, UUID idAfter, int limit) {
        QNotification notification = QNotification.notification;

        JPAQuery<Notification> query = queryFactory.selectFrom(notification)
            .where(notification.receiverId.eq(userId));

        if (cursor != null && idAfter != null) {
            query.where(notification.createdAt.lt(cursor)
                .or(notification.createdAt.eq(cursor))
                .and(notification.id.lt(idAfter)));
        }

        return query
            .orderBy(notification.createdAt.desc())
            .limit(limit + 1)
            .fetch();
    }

}
