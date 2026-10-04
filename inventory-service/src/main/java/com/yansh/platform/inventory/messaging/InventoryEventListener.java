package com.yansh.platform.inventory.messaging;

import com.yansh.platform.common.events.InventoryFailedEvent;
import com.yansh.platform.common.events.InventoryReservedEvent;
import com.yansh.platform.common.events.OrderCreatedEvent;
import com.yansh.platform.common.events.PaymentFailedEvent;
import com.yansh.platform.common.messaging.EventPublisher;
import com.yansh.platform.common.messaging.Topics;
import com.yansh.platform.inventory.domain.Reservation;
import com.yansh.platform.inventory.domain.ReservationStatus;
import com.yansh.platform.inventory.service.InventoryService;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventListener {

    private final InventoryService inventoryService;
    private final EventPublisher publisher;

    public InventoryEventListener(InventoryService inventoryService, EventPublisher publisher) {
        this.inventoryService = inventoryService;
        this.publisher = publisher;
    }

    /**
     * The DB transaction commits inside reserve(); the outcome event is published afterwards.
     * If we crash in between, redelivery finds the stored reservation and simply republishes.
     */
    @KafkaListener(topics = Topics.ORDERS_CREATED)
    public void onOrderCreated(OrderCreatedEvent event) {
        Reservation reservation = inventoryService.reserve(event);

        if (reservation.getStatus() == ReservationStatus.RESERVED) {
            publisher.publish(Topics.INVENTORY_RESERVED, event.orderId(),
                    new InventoryReservedEvent(UUID.randomUUID().toString(), event.orderId(),
                            reservation.getUserId(), reservation.getTotalAmount()));
        } else if (reservation.getStatus() == ReservationStatus.FAILED) {
            publisher.publish(Topics.INVENTORY_FAILED, event.orderId(),
                    new InventoryFailedEvent(UUID.randomUUID().toString(), event.orderId(), reservation.getReason()));
        }
    }

    /** Saga compensation. */
    @KafkaListener(topics = Topics.PAYMENTS_FAILED)
    public void onPaymentFailed(PaymentFailedEvent event) {
        inventoryService.release(event.orderId());
    }
}
