package com.yansh.platform.inventory.repository;

import com.yansh.platform.inventory.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, String> {
}
