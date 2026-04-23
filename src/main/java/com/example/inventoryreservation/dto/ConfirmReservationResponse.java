package com.example.inventoryreservation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConfirmReservationResponse(
    UUID reservationId,
    String status,
    Instant confirmedAt,
    List<InventoryEffect> inventoryEffects
) {}
