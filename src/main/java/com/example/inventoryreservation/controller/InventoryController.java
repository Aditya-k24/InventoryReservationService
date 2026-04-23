package com.example.inventoryreservation.controller;

import com.example.inventoryreservation.dto.AdjustInventoryRequest;
import com.example.inventoryreservation.dto.InventoryResponse;
import com.example.inventoryreservation.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponse> adjust(@Valid @RequestBody AdjustInventoryRequest request) {
        return ResponseEntity.ok(inventoryService.adjustInventory(request));
    }

    @GetMapping("/{skuCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable String skuCode) {
        return ResponseEntity.ok(inventoryService.getInventory(skuCode));
    }
}
