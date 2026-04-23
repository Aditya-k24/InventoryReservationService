package com.example.inventoryreservation.dto;

public record InventoryEffect(
    String skuCode,
    int deltaTotal,
    int deltaAvailable,
    int deltaReserved
) {}
