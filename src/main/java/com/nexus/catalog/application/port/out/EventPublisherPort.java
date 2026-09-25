package com.nexus.catalog.application.port.out;

import com.nexus.common.events.DomainEvent;

public interface EventPublisherPort {
    void publish(DomainEvent event);
}
