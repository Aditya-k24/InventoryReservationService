package com.example.inventoryreservation.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "reservation_item")
public class ReservationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    private Sku sku;

    @Column(nullable = false)
    private int quantity;

    protected ReservationItem() {}

    public ReservationItem(Reservation reservation, Sku sku, int quantity) {
        this.reservation = reservation;
        this.sku = sku;
        this.quantity = quantity;
    }

    public UUID getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public Sku getSku() { return sku; }
    public int getQuantity() { return quantity; }
}
