package com.yansh.platform.payment.web;

import com.yansh.platform.payment.domain.Payment;
import com.yansh.platform.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public record PaymentView(String orderId, String paymentId, String status, String reason) {
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentView> get(@PathVariable String orderId) {
        Payment p = paymentService.get(orderId);
        if (p == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new PaymentView(p.getOrderId(), p.getPaymentId(), p.getStatus().name(), p.getReason()));
    }
}
