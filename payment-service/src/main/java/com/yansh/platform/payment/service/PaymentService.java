package com.yansh.platform.payment.service;

import com.yansh.platform.common.events.InventoryReservedEvent;
import com.yansh.platform.payment.domain.Payment;
import com.yansh.platform.payment.domain.PaymentStatus;
import com.yansh.platform.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentGateway gateway;

    public PaymentService(PaymentRepository paymentRepository, PaymentGateway gateway) {
        this.paymentRepository = paymentRepository;
        this.gateway = gateway;
    }

    /** Charges once per order. A redelivered event returns the stored result without charging again. */
    @Transactional
    public Payment process(InventoryReservedEvent event) {
        return paymentRepository.findById(event.orderId()).orElseGet(() -> {
            PaymentGateway.ChargeResult result = gateway.charge(event.totalAmount());
            Payment payment = new Payment(event.orderId(), event.totalAmount(),
                    result.success() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED, result.reason());
            log.info("Payment for order {} -> {}", event.orderId(), payment.getStatus());
            return paymentRepository.save(payment);
        });
    }

    @Transactional(readOnly = true)
    public Payment get(String orderId) {
        return paymentRepository.findById(orderId).orElse(null);
    }
}
