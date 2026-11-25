package com.mirpur.advising.shared.event;



public interface EventPublisher {
    void publish(DomainEvent event);
}
