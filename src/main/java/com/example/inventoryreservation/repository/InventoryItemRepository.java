package com.example.inventoryreservation.repository;

import com.example.inventoryreservation.domain.InventoryItem;
import com.example.inventoryreservation.domain.Sku;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findBySku(Sku sku);

    @Query("SELECT i FROM InventoryItem i JOIN FETCH i.sku WHERE i.sku.skuCode = :skuCode")
    Optional<InventoryItem> findBySkuCode(@Param("skuCode") String skuCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i JOIN FETCH i.sku s WHERE s.skuCode IN :skuCodes ORDER BY s.skuCode")
    List<InventoryItem> findBySkuCodesWithLock(@Param("skuCodes") List<String> skuCodes);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i JOIN FETCH i.sku s WHERE s.skuCode = :skuCode")
    Optional<InventoryItem> findBySkuCodeWithLock(@Param("skuCode") String skuCode);
}
