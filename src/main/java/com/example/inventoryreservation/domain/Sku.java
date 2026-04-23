package com.example.inventoryreservation.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sku")
public class Sku {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sku_code", nullable = false, unique = true, length = 64)
    private String skuCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Sku() {}

    public Sku(String skuCode, String name) {
        this.skuCode = skuCode;
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getSkuCode() { return skuCode; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
