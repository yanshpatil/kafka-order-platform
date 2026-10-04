package com.yansh.platform.order.messaging;

import com.yansh.platform.common.messaging.EventPublisher;
import com.yansh.platform.order.domain.OutboxEvent;
import com.yansh.platform.order.repository.OutboxRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls the outbox table and relays rows to Kafka in creation order. Delivery is at-least-once:
 * if the app crashes after a send but before the row is marked published, the event is sent
 * again, which is safe because every consumer is idempotent.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final EventPublisher eventPublisher;

    public OutboxPublisher(OutboxRepository outboxRepository, EventPublisher eventPublisher) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            try {
                eventPublisher.publishRaw(event.getTopic(), event.getAggregateId(), event.getPayload());
                event.markPublished();
            } catch (RuntimeException e) {
                log.warn("Outbox publish failed for {}, will retry on next poll: {}", event.getId(), e.getMessage());
                break; // stop here to preserve ordering
            }
        }
    }
}
