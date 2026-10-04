package com.yansh.platform.inventory.service;

import com.yansh.platform.common.events.OrderCreatedEvent;
import com.yansh.platform.inventory.domain.Reservation;
import com.yansh.platform.inventory.domain.ReservationLine;
import com.yansh.platform.inventory.domain.ReservationStatus;
import com.yansh.platform.inventory.domain.Stock;
import com.yansh.platform.inventory.repository.ReservationRepository;
import com.yansh.platform.inventory.repository.StockRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;

    public InventoryService(StockRepository stockRepository, ReservationRepository reservationRepository) {
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Reserves stock for an order, all-or-nothing. If a reservation already exists for the
     * order it is returned unchanged, so redelivered events never reserve twice.
     */
    @Transactional
    public Reservation reserve(OrderCreatedEvent event) {
        return reservationRepository.findById(event.orderId()).orElseGet(() -> doReserve(event));
    }

    private Reservation doReserve(OrderCreatedEvent event) {
        Reservation reservation = new Reservation(event.orderId(), event.userId(), event.totalAmount());

        // Combine duplicate product lines so the stock check is accurate
        Map<String, Integer> needed = new LinkedHashMap<>();
        event.items().forEach(i -> needed.merge(i.productId(), i.quantity(), Integer::sum));

        List<Stock> toUpdate = new ArrayList<>();
        List<ReservationLine> lines = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : needed.entrySet()) {
            Stock stock = stockRepository.findById(entry.getKey()).orElse(null);
            if (stock == null) {
                return failed(reservation, "Unknown product " + entry.getKey());
            }
            if (stock.getQuantity() < entry.getValue()) {
                return failed(reservation, "Insufficient stock for " + entry.getKey()
                        + " (available " + stock.getQuantity() + ", requested " + entry.getValue() + ")");
            }
            toUpdate.add(stock);
            lines.add(new ReservationLine(entry.getKey(), entry.getValue()));
        }

        for (int i = 0; i < toUpdate.size(); i++) {
            Stock stock = toUpdate.get(i);
            stock.setQuantity(stock.getQuantity() - lines.get(i).getQuantity());
        }
        stockRepository.saveAll(toUpdate);

        reservation.markReserved(lines);
        log.info("Reserved stock for order {}", event.orderId());
        return reservationRepository.save(reservation);
    }

    private Reservation failed(Reservation reservation, String reason) {
        reservation.markFailed(reason);
        log.warn("Reservation failed for order {}: {}", reservation.getOrderId(), reason);
        return reservationRepository.save(reservation);
    }

    /** Compensating action for a failed payment: give the reserved stock back. Idempotent. */
    @Transactional
    public void release(String orderId) {
        reservationRepository.findById(orderId)
                .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                .ifPresent(r -> {
                    for (ReservationLine line : r.getLines()) {
                        stockRepository.findById(line.getProductId()).ifPresent(
                                s -> s.setQuantity(s.getQuantity() + line.getQuantity()));
                    }
                    r.markReleased();
                    log.info("Released stock for order {} (compensation)", orderId);
                });
    }

    @Transactional(readOnly = true)
    public List<Stock> allStock() {
        return stockRepository.findAll();
    }
}
