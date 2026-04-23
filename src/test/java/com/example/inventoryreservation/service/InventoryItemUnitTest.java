package com.example.inventoryreservation.service;

import com.example.inventoryreservation.domain.InventoryItem;
import com.example.inventoryreservation.domain.Sku;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class InventoryItemUnitTest {

    private InventoryItem item;

    @BeforeEach
    void setUp() {
        Sku sku = new Sku("SKU-001", "Test Item");
        item = new InventoryItem(sku, 10);
    }

    @Test
    void reserveReducesAvailableAndIncreasesReserved() {
        item.reserve(3);
        assertThat(item.getTotalQty()).isEqualTo(10);
        assertThat(item.getAvailableQty()).isEqualTo(7);
        assertThat(item.getReservedQty()).isEqualTo(3);
    }

    @Test
    void reserveThrowsWhenInsufficientStock() {
        assertThatThrownBy(() -> item.reserve(11))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void confirmReducesReservedAndTotal() {
        item.reserve(3);
        item.confirm(3);
        assertThat(item.getTotalQty()).isEqualTo(7);
        assertThat(item.getAvailableQty()).isEqualTo(7);
        assertThat(item.getReservedQty()).isEqualTo(0);
    }

    @Test
    void releaseRestoresAvailableFromReserved() {
        item.reserve(4);
        item.release(4);
        assertThat(item.getTotalQty()).isEqualTo(10);
        assertThat(item.getAvailableQty()).isEqualTo(10);
        assertThat(item.getReservedQty()).isEqualTo(0);
    }

    @Test
    void positiveAdjustIncreasesTotalAndAvailable() {
        item.adjust(5);
        assertThat(item.getTotalQty()).isEqualTo(15);
        assertThat(item.getAvailableQty()).isEqualTo(15);
        assertThat(item.getReservedQty()).isEqualTo(0);
    }

    @Test
    void negativeAdjustReducesTotalAndAvailable() {
        item.adjust(-5);
        assertThat(item.getTotalQty()).isEqualTo(5);
        assertThat(item.getAvailableQty()).isEqualTo(5);
    }

    @Test
    void adjustThrowsWhenAvailableWouldGoNegative() {
        item.reserve(8);
        assertThatThrownBy(() -> item.adjust(-5))
            .isInstanceOf(IllegalStateException.class);
    }
}
