package com.yansh.platform.payment.service;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Simulated payment provider with a configurable decline rate and amount limit. */
@Component
public class PaymentGateway {

    public record ChargeResult(boolean success, String reason) {
    }

    private final double failureRate;
    private final BigDecimal maxAmount;

    public PaymentGateway(@Value("${payment.failure-rate:0.1}") double failureRate,
                          @Value("${payment.max-amount:100000}") BigDecimal maxAmount) {
        this.failureRate = failureRate;
        this.maxAmount = maxAmount;
    }

    public ChargeResult charge(BigDecimal amount) {
        if (amount.compareTo(maxAmount) > 0) {
            return new ChargeResult(false, "Amount exceeds the allowed limit of " + maxAmount);
        }
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            return new ChargeResult(false, "Card declined (simulated)");
        }
        return new ChargeResult(true, null);
    }
}
