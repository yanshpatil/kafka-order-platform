package com.yansh.platform.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yansh.platform.common.events.OrderCreatedEvent;
import com.yansh.platform.common.events.OrderItem;
import com.yansh.platform.common.messaging.Topics;
import com.yansh.platform.order.domain.Order;
import com.yansh.platform.order.domain.OrderLine;
import com.yansh.platform.order.domain.OrderStatus;
import com.yansh.platform.order.domain.OutboxEvent;
import com.yansh.platform.order.repository.OrderRepository;
import com.yansh.platform.order.repository.OutboxRepository;
import com.yansh.platform.order.web.CreateOrderRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Saves the order and its OrderCreated outbox row in ONE transaction. A separate poller
     * publishes the row to Kafka, so the database and the broker can never disagree.
     */
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        List<OrderLine> lines = request.items().stream()
                .map(i -> new OrderLine(i.productId(), i.quantity(), i.price()))
                .toList();

        BigDecimal total = lines.stream()
                .map(l -> l.getPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = orderRepository.save(new Order(UUID.randomUUID().toString(), request.userId(), total, lines));

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                order.getId(),
                order.getUserId(),
                lines.stream().map(l -> new OrderItem(l.getProductId(), l.getQuantity(), l.getPrice())).toList(),
                total,
                Instant.now());

        try {
            outboxRepository.save(new OutboxEvent(order.getId(), Topics.ORDERS_CREATED,
                    objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize OrderCreatedEvent", e);
        }
        log.info("Order {} created for user {} total={}", order.getId(), order.getUserId(), total);
        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersForUser(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Idempotent: only a PENDING order can change state, so redelivered events are harmless. */
    @Transactional
    public void confirm(String orderId) {
        orderRepository.findById(orderId)
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .ifPresent(o -> {
                    o.confirm();
                    log.info("Order {} CONFIRMED", orderId);
                });
    }

    @Transactional
    public void cancel(String orderId, String reason) {
        orderRepository.findById(orderId)
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .ifPresent(o -> {
                    o.cancel(reason);
                    log.info("Order {} CANCELLED: {}", orderId, reason);
                });
    }
}
