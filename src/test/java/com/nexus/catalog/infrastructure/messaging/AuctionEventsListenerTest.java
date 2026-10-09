package com.nexus.catalog.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.catalog.application.usecase.MarkProductSoldUseCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuctionEventsListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final MarkProductSoldUseCase markProductSoldUseCase = mock(MarkProductSoldUseCase.class);
    private final AuctionEventsListener listener = new AuctionEventsListener(objectMapper, markProductSoldUseCase);

    @Test
    void onMessage_auctionWon_marksTheProductSold() {
        String payload = "{\"eventId\":\"e-1\",\"eventType\":\"AuctionWon\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"auction-1\",\"auctionId\":\"auction-1\",\"productId\":\"product-1\","
                + "\"sellerId\":\"seller-1\",\"winnerId\":\"winner-1\",\"finalPrice\":500.00}";

        listener.onMessage(payload);

        verify(markProductSoldUseCase).markSold("product-1");
    }

    @Test
    void onMessage_otherEventType_doesNothing() {
        String payload = "{\"eventId\":\"e-2\",\"eventType\":\"BidPlaced\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"auction-1\",\"auctionId\":\"auction-1\",\"bidderId\":\"bidder-1\","
                + "\"amount\":150.00}";

        listener.onMessage(payload);

        verify(markProductSoldUseCase, never()).markSold(any());
    }

    @Test
    void onMessage_malformedPayload_doesNotThrow() {
        assertThatCode(() -> listener.onMessage("{not valid json")).doesNotThrowAnyException();

        verify(markProductSoldUseCase, never()).markSold(any());
    }
}
