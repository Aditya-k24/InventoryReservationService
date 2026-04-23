package com.example.inventoryreservation.repository;

import com.example.inventoryreservation.domain.AppUser;
import com.example.inventoryreservation.domain.Reservation;
import com.example.inventoryreservation.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByCreatedByUserAndIdempotencyKey(AppUser user, String idempotencyKey);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'PENDING' AND r.expiresAt <= :now")
    List<Reservation> findPendingExpired(@Param("now") Instant now);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.items i JOIN FETCH i.sku WHERE r.id = :id")
    Optional<Reservation> findByIdWithItems(@Param("id") UUID id);
}
