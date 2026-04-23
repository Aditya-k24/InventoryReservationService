package com.example.inventoryreservation.controller;

import com.example.inventoryreservation.dto.*;
import com.example.inventoryreservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CreateReservationResponse> createReservation(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody CreateReservationRequest request) {

        CreateReservationResponse response = reservationService
            .createReservation(userDetails.getUsername(), idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(reservationService.getReservation(reservationId));
    }

    @PostMapping("/{reservationId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ConfirmReservationResponse> confirmReservation(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(reservationService.confirmReservation(reservationId));
    }

    @PostMapping("/{reservationId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(reservationService.cancelReservation(reservationId));
    }
}
