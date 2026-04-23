package com.example.inventoryreservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdjustInventoryRequest(
    @NotBlank String skuCode,
    @NotNull Integer delta,
    String reason
) {}
