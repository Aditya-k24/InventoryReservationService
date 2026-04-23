package com.example.inventoryreservation.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_event")
public class InventoryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    private Sku sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "delta_total", nullable = false)
    private int deltaTotal;

    @Column(name = "delta_available", nullable = false)
    private int deltaAvailable;

    @Column(name = "delta_reserved", nullable = false)
    private int deltaReserved;

    @Column(columnDefinition = "text")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InventoryEvent() {}

    public InventoryEvent(Sku sku, Reservation reservation, String eventType,
                          int deltaTotal, int deltaAvailable, int deltaReserved, String metadata) {
        this.sku = sku;
        this.reservation = reservation;
        this.eventType = eventType;
        this.deltaTotal = deltaTotal;
        this.deltaAvailable = deltaAvailable;
        this.deltaReserved = deltaReserved;
        this.metadata = metadata;
    }

    public UUID getId() { return id; }
    public Sku getSku() { return sku; }
    public Reservation getReservation() { return reservation; }
    public String getEventType() { return eventType; }
    public int getDeltaTotal() { return deltaTotal; }
    public int getDeltaAvailable() { return deltaAvailable; }
    public int getDeltaReserved() { return deltaReserved; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
}
