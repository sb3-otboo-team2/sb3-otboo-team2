package org.ikuzo.otboo.global.event.message;

import lombok.Getter;

import java.time.Instant;

@Getter
public abstract class CreatedEvent<T> {

    private final T dto;
    private final Instant createdAt;

    protected CreatedEvent(T dto, Instant createdAt) {
        this.dto = dto;
        this.createdAt = createdAt;
    }
}
