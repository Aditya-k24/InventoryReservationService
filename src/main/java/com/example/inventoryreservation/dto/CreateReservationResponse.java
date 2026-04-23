package com.example.inventoryreservation.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateReservationResponse(
    UUID reservationId,
    String status,
    String customerReference,
    Instant expiresAt,
    List<ReservationItemResponse> items,
    Map<String, String> links
) {}
