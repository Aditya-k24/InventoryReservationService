package com.example.inventoryreservation.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservation",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_reservation_idempotency",
        columnNames = {"created_by_user_id", "idempotency_key"}
    ))
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private AppUser createdByUser;

    @Column(name = "customer_reference", nullable = false, length = 128)
    private String customerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationItem> items = new ArrayList<>();

    protected Reservation() {}

    public Reservation(AppUser createdByUser, String customerReference,
                       String idempotencyKey, Instant expiresAt) {
        this.createdByUser = createdByUser;
        this.customerReference = customerReference;
        this.idempotencyKey = idempotencyKey;
        this.expiresAt = expiresAt;
    }

    public void confirm(Instant now) {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Can only confirm a PENDING reservation");
        }
        if (now.isAfter(expiresAt)) {
            throw new IllegalStateException("Cannot confirm an expired reservation");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = now;
    }

    public void cancel(Instant now) {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Can only cancel a PENDING reservation");
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = now;
    }

    public void expire() {
        this.status = ReservationStatus.EXPIRED;
    }

    public void addItem(ReservationItem item) {
        items.add(item);
    }

    public UUID getId() { return id; }
    public AppUser getCreatedByUser() { return createdByUser; }
    public String getCustomerReference() { return customerReference; }
    public ReservationStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public List<ReservationItem> getItems() { return items; }
}
