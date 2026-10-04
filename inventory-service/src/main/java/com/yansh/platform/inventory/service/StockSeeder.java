package com.yansh.platform.inventory.service;

import com.yansh.platform.inventory.domain.Stock;
import com.yansh.platform.inventory.repository.StockRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Seeds demo products on first start so the platform can be tried immediately. */
@Component
public class StockSeeder implements CommandLineRunner {

    private final StockRepository stockRepository;

    public StockSeeder(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public void run(String... args) {
        if (stockRepository.count() == 0) {
            stockRepository.saveAll(List.of(
                    new Stock("P1001", 100),
                    new Stock("P1002", 100),
                    new Stock("P1003", 100),
                    new Stock("P1004", 100),
                    new Stock("P1005", 5)));
        }
    }
}
