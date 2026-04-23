package com.example.inventoryreservation.repository;

import com.example.inventoryreservation.domain.InventoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryEventRepository extends JpaRepository<InventoryEvent, UUID> {
}
