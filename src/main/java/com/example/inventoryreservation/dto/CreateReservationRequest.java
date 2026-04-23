package com.example.inventoryreservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateReservationRequest(
    @NotBlank String customerReference,
    @Positive int ttlMinutes,
    @NotEmpty @Valid List<ReservationItemRequest> items
) {}
