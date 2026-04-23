package com.example.inventoryreservation.service;

import com.example.inventoryreservation.domain.InventoryEvent;
import com.example.inventoryreservation.domain.InventoryItem;
import com.example.inventoryreservation.domain.Sku;
import com.example.inventoryreservation.dto.AdjustInventoryRequest;
import com.example.inventoryreservation.dto.InventoryResponse;
import com.example.inventoryreservation.exception.ResourceNotFoundException;
import com.example.inventoryreservation.repository.InventoryEventRepository;
import com.example.inventoryreservation.repository.InventoryItemRepository;
import com.example.inventoryreservation.repository.SkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final SkuRepository skuRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryEventRepository inventoryEventRepository;

    public InventoryService(SkuRepository skuRepository,
                            InventoryItemRepository inventoryItemRepository,
                            InventoryEventRepository inventoryEventRepository) {
        this.skuRepository = skuRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryEventRepository = inventoryEventRepository;
    }

    @Transactional
    public InventoryResponse adjustInventory(AdjustInventoryRequest request) {
        if (request.delta() == 0) {
            throw new IllegalArgumentException("Delta must be non-zero");
        }
        InventoryItem item = inventoryItemRepository.findBySkuCodeWithLock(request.skuCode())
            .orElseGet(() -> {
                Sku sku = skuRepository.findBySkuCode(request.skuCode())
                    .orElseThrow(() -> new ResourceNotFoundException("SKU not found: " + request.skuCode()));
                return inventoryItemRepository.save(new InventoryItem(sku, 0));
            });

        item.adjust(request.delta());

        InventoryEvent event = new InventoryEvent(
            item.getSku(), null, "ADJUSTMENT",
            request.delta(), request.delta(), 0,
            "{\"reason\":\"%s\"}".formatted(request.reason() != null ? request.reason() : "")
        );
        inventoryEventRepository.save(event);

        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(String skuCode) {
        InventoryItem item = inventoryItemRepository.findBySkuCode(skuCode)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU: " + skuCode));
        return toResponse(item);
    }

    public static InventoryResponse toResponse(InventoryItem item) {
        return new InventoryResponse(
            item.getSku().getSkuCode(),
            item.getSku().getName(),
            item.getTotalQty(),
            item.getAvailableQty(),
            item.getReservedQty(),
            item.getUpdatedAt()
        );
    }
}
