package com.example.inventoryreservation.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReservationResponse(
    UUID reservationId,
    String status,
    String customerReference,
    Instant expiresAt,
    Instant confirmedAt,
    Instant cancelledAt,
    List<ReservationItemResponse> items,
    Map<String, String> links
) {}
