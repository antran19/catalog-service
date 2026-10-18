package com.nexus.catalog.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.catalog.application.usecase.MarkProductSoldUseCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuctionEventsListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final MarkProductSoldUseCase markProductSoldUseCase = mock(MarkProductSoldUseCase.class);
    private final com.nexus.catalog.application.usecase.RelistProductUseCase relistProductUseCase =
            mock(com.nexus.catalog.application.usecase.RelistProductUseCase.class);
    private final AuctionEventsListener listener = new AuctionEventsListener(objectMapper, markProductSoldUseCase,
            relistProductUseCase);

    @Test
    void onMessage_auctionWon_marksTheProductSold() throws Exception {
        String payload = "{\"eventId\":\"e-1\",\"eventType\":\"AuctionWon\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"auction-1\",\"auctionId\":\"auction-1\",\"productId\":\"product-1\","
                + "\"sellerId\":\"seller-1\",\"winnerId\":\"winner-1\",\"finalPrice\":500.00}";

        listener.onMessage(payload);

        verify(markProductSoldUseCase).markSold("product-1");
    }

    @Test
    void onMessage_otherEventType_doesNothing() throws Exception {
        String payload = "{\"eventId\":\"e-2\",\"eventType\":\"BidPlaced\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"auction-1\",\"auctionId\":\"auction-1\",\"bidderId\":\"bidder-1\","
                + "\"amount\":150.00}";

        listener.onMessage(payload);

        verify(markProductSoldUseCase, never()).markSold(any());
    }

    @Test
    void onMessage_malformedPayload_throwsSoItGoesToDeadLetterTopic() throws Exception {
        assertThatThrownBy(() -> listener.onMessage("{not valid json"))
                .isInstanceOf(JsonProcessingException.class);

        verify(markProductSoldUseCase, never()).markSold(any());
    }

    @Test
    void onMessage_winnerDefaulted_putsTheProductBackOnSale() throws Exception {
        listener.onMessage("{\"eventId\":\"e\",\"eventType\":\"AuctionPaymentTimeout\",\"occurredAt\":\"2026-01-01T00:00:00Z\","
                + "\"aggregateId\":\"a-1\",\"auctionId\":\"a-1\",\"winnerId\":\"w\",\"productId\":\"p-1\"}");

        verify(relistProductUseCase).relist("p-1");
    }
}
