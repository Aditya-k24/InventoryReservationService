package com.example.inventoryreservation.repository;

import com.example.inventoryreservation.domain.Sku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkuRepository extends JpaRepository<Sku, UUID> {
    Optional<Sku> findBySkuCode(String skuCode);
    List<Sku> findBySkuCodeIn(List<String> skuCodes);
}
