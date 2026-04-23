package com.example.inventoryreservation.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_item")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false, unique = true)
    private Sku sku;

    @Column(name = "total_qty", nullable = false)
    private int totalQty;

    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {}

    public InventoryItem(Sku sku, int totalQty) {
        this.sku = sku;
        this.totalQty = totalQty;
        this.availableQty = totalQty;
        this.reservedQty = 0;
    }

    public void reserve(int qty) {
        if (availableQty < qty) {
            throw new IllegalStateException("Insufficient available quantity");
        }
        availableQty -= qty;
        reservedQty += qty;
    }

    public void confirm(int qty) {
        if (reservedQty < qty) {
            throw new IllegalStateException("Insufficient reserved quantity");
        }
        reservedQty -= qty;
        totalQty -= qty;
    }

    public void release(int qty) {
        if (reservedQty < qty) {
            throw new IllegalStateException("Insufficient reserved quantity to release");
        }
        reservedQty -= qty;
        availableQty += qty;
    }

    public void adjust(int delta) {
        if (delta < 0 && availableQty + delta < 0) {
            throw new IllegalStateException("Adjustment would make available quantity negative");
        }
        if (totalQty + delta < 0) {
            throw new IllegalStateException("Adjustment would make total quantity negative");
        }
        totalQty += delta;
        availableQty += delta;
    }

    public UUID getId() { return id; }
    public Sku getSku() { return sku; }
    public int getTotalQty() { return totalQty; }
    public int getAvailableQty() { return availableQty; }
    public int getReservedQty() { return reservedQty; }
    public Instant getUpdatedAt() { return updatedAt; }
}
