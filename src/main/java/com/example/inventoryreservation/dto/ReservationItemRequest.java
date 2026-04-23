package com.example.inventoryreservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ReservationItemRequest(
    @NotBlank String skuCode,
    @Positive int quantity
) {}
