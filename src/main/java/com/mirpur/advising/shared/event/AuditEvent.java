package com.mirpur.advising.shared.event;



public record AuditEvent(Long actor, String action, String entity, Long id, String detail) implements DomainEvent {
}
