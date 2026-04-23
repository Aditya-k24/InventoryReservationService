package com.example.inventoryreservation.dto;

import java.time.Instant;

public record InventoryResponse(
    String skuCode,
    String name,
    int totalQuantity,
    int availableQuantity,
    int reservedQuantity,
    Instant updatedAt
) {}
