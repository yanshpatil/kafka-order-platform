package com.yansh.platform.payment.messaging;

import com.yansh.platform.common.events.InventoryReservedEvent;
import com.yansh.platform.common.events.PaymentCompletedEvent;
import com.yansh.platform.common.events.PaymentFailedEvent;
import com.yansh.platform.common.messaging.EventPublisher;
import com.yansh.platform.common.messaging.Topics;
import com.yansh.platform.payment.domain.Payment;
import com.yansh.platform.payment.domain.PaymentStatus;
import com.yansh.platform.payment.service.PaymentService;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private final PaymentService paymentService;
    private final EventPublisher publisher;

    public PaymentEventListener(PaymentService paymentService, EventPublisher publisher) {
        this.paymentService = paymentService;
        this.publisher = publisher;
    }

    @KafkaListener(topics = Topics.INVENTORY_RESERVED)
    public void onInventoryReserved(InventoryReservedEvent event) {
        Payment payment = paymentService.process(event);

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            publisher.publish(Topics.PAYMENTS_COMPLETED, event.orderId(),
                    new PaymentCompletedEvent(UUID.randomUUID().toString(), event.orderId(), payment.getPaymentId()));
        } else {
            publisher.publish(Topics.PAYMENTS_FAILED, event.orderId(),
                    new PaymentFailedEvent(UUID.randomUUID().toString(), event.orderId(), payment.getReason()));
        }
    }
}
