package com.mirpur.advising.shared.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component public final class SynchronousEventPublisher implements EventPublisher {
    private final ApplicationEventPublisher events;
    public SynchronousEventPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }
    public void publish(DomainEvent event) {
        events.publishEvent(event);
    }
}
