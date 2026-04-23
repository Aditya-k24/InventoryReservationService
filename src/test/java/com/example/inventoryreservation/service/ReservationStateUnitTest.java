package com.example.inventoryreservation.service;

import com.example.inventoryreservation.domain.AppUser;
import com.example.inventoryreservation.domain.Reservation;
import com.example.inventoryreservation.domain.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

class ReservationStateUnitTest {

    private AppUser dummyUser() {
        return new AppUser("test@example.com", "hash", "OPERATOR");
    }

    @Test
    void confirmTransitionsToPending() {
        Reservation r = new Reservation(dummyUser(), "REF-001", null,
            Instant.now().plus(15, ChronoUnit.MINUTES));
        r.confirm(Instant.now());
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(r.getConfirmedAt()).isNotNull();
    }

    @Test
    void cancelTransitionsToCancelled() {
        Reservation r = new Reservation(dummyUser(), "REF-002", null,
            Instant.now().plus(15, ChronoUnit.MINUTES));
        r.cancel(Instant.now());
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(r.getCancelledAt()).isNotNull();
    }

    @Test
    void cannotConfirmExpiredReservation() {
        Reservation r = new Reservation(dummyUser(), "REF-003", null,
            Instant.now().minus(1, ChronoUnit.MINUTES));
        assertThatThrownBy(() -> r.confirm(Instant.now()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("expired");
    }

    @Test
    void cannotConfirmAlreadyCancelledReservation() {
        Reservation r = new Reservation(dummyUser(), "REF-004", null,
            Instant.now().plus(15, ChronoUnit.MINUTES));
        r.cancel(Instant.now());
        assertThatThrownBy(() -> r.confirm(Instant.now()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannotCancelAlreadyConfirmedReservation() {
        Reservation r = new Reservation(dummyUser(), "REF-005", null,
            Instant.now().plus(15, ChronoUnit.MINUTES));
        r.confirm(Instant.now());
        assertThatThrownBy(() -> r.cancel(Instant.now()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void expireTransitionsToExpired() {
        Reservation r = new Reservation(dummyUser(), "REF-006", null,
            Instant.now().minus(1, ChronoUnit.MINUTES));
        r.expire();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }
}
