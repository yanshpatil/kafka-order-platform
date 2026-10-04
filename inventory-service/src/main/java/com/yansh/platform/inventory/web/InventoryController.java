package com.yansh.platform.inventory.web;

import com.yansh.platform.inventory.service.InventoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public record StockView(String productId, int quantity) {
    }

    @GetMapping
    public List<StockView> stock() {
        return inventoryService.allStock().stream()
                .map(s -> new StockView(s.getProductId(), s.getQuantity()))
                .toList();
    }
}
