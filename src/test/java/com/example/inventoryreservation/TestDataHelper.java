package com.example.inventoryreservation;

import com.example.inventoryreservation.domain.AppUser;
import com.example.inventoryreservation.domain.InventoryItem;
import com.example.inventoryreservation.domain.Sku;
import com.example.inventoryreservation.repository.AppUserRepository;
import com.example.inventoryreservation.repository.InventoryItemRepository;
import com.example.inventoryreservation.repository.SkuRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TestDataHelper {

    private final AppUserRepository userRepository;
    private final SkuRepository skuRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PasswordEncoder passwordEncoder;

    public TestDataHelper(AppUserRepository userRepository,
                          SkuRepository skuRepository,
                          InventoryItemRepository inventoryItemRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.skuRepository = skuRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser createAdmin() {
        return userRepository.findByEmail("admin@example.com")
            .orElseGet(() -> userRepository.save(
                new AppUser("admin@example.com", passwordEncoder.encode("changeme"), "ADMIN")));
    }

    public AppUser createOperator() {
        return userRepository.findByEmail("ops@example.com")
            .orElseGet(() -> userRepository.save(
                new AppUser("ops@example.com", passwordEncoder.encode("changeme"), "OPERATOR")));
    }

    public InventoryItem createInventory(String skuCode, String name, int qty) {
        Sku sku = skuRepository.findBySkuCode(skuCode)
            .orElseGet(() -> skuRepository.save(new Sku(skuCode, name)));
        return inventoryItemRepository.findBySku(sku)
            .orElseGet(() -> inventoryItemRepository.save(new InventoryItem(sku, qty)));
    }
}
