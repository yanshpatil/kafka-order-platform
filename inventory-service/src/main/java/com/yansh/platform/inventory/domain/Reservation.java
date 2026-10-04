package com.yansh.platform.inventory.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One row per order. Its existence makes the consumer idempotent (a redelivered OrderCreated
 * finds the row instead of reserving twice) and records what to give back if payment fails.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reservation_lines", joinColumns = @JoinColumn(name = "order_id"))
    private List<ReservationLine> lines = new ArrayList<>();

    protected Reservation() {
    }

    public Reservation(String orderId, String userId, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.createdAt = Instant.now();
    }

    public void markReserved(List<ReservationLine> reservedLines) {
        this.status = ReservationStatus.RESERVED;
        this.lines = new ArrayList<>(reservedLines);
    }

    public void markFailed(String reason) {
        this.status = ReservationStatus.FAILED;
        this.reason = reason;
    }

    public void markReleased() {
        this.status = ReservationStatus.RELEASED;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public List<ReservationLine> getLines() {
        return lines;
    }
}
