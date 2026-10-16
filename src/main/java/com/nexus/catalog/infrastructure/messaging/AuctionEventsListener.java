package com.nexus.catalog.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.catalog.application.usecase.MarkProductSoldUseCase;
import com.nexus.common.events.AuctionWonEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Without this, a product whose auction ended with a winner kept showing ACTIVE in
// search/discover forever -- nothing in this service ever learned the auction concluded.
@Component
public class AuctionEventsListener {

    private static final Logger log = LoggerFactory.getLogger(AuctionEventsListener.class);

    private final ObjectMapper objectMapper;
    private final MarkProductSoldUseCase markProductSoldUseCase;

    public AuctionEventsListener(ObjectMapper objectMapper, MarkProductSoldUseCase markProductSoldUseCase) {
        this.objectMapper = objectMapper;
        this.markProductSoldUseCase = markProductSoldUseCase;
    }

    // Exceptions are NOT caught here on purpose: KafkaErrorHandlingConfig retries them, then
    // moves the record to the dead-letter topic instead of losing it.
    @KafkaListener(topics = "auction-events")
    public void onMessage(String rawPayload) throws JsonProcessingException {
        String eventType = objectMapper.readTree(rawPayload).path("eventType").asText();
        if (!"AuctionWon".equals(eventType)) {
            return;
        }
        AuctionWonEvent event = objectMapper.readValue(rawPayload, AuctionWonEvent.class);
        markProductSoldUseCase.markSold(event.getProductId());
    }
}
