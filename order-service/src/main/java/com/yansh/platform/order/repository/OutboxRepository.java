package com.yansh.platform.order.repository;

import com.yansh.platform.order.domain.OutboxEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
