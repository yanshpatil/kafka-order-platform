package com.yansh.platform.order.messaging;

import com.yansh.platform.common.events.InventoryFailedEvent;
import com.yansh.platform.common.events.PaymentCompletedEvent;
import com.yansh.platform.common.events.PaymentFailedEvent;
import com.yansh.platform.common.messaging.Topics;
import com.yansh.platform.order.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Closes the saga: turns the downstream outcome events into the final order status. */
@Component
public class OrderEventListener {

    private final OrderService orderService;

    public OrderEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = Topics.PAYMENTS_COMPLETED)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        orderService.confirm(event.orderId());
    }

    @KafkaListener(topics = Topics.PAYMENTS_FAILED)
    public void onPaymentFailed(PaymentFailedEvent event) {
        orderService.cancel(event.orderId(), "Payment failed: " + event.reason());
    }

    @KafkaListener(topics = Topics.INVENTORY_FAILED)
    public void onInventoryFailed(InventoryFailedEvent event) {
        orderService.cancel(event.orderId(), "Inventory unavailable: " + event.reason());
    }
}
