package com.example.inventoryreservation.exception;

public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(String skuCode, int requested, int available) {
        super("Requested %d units of %s but only %d are available.".formatted(requested, skuCode, available));
    }
}
